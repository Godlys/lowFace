package com.low.face;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.Log;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/**
 * MiniVision 静默活体检测器
 *
 * 基于 MiniFASNetV2 TFLite 模型实现 2D 静默活体检测
 * 防御静态照片与屏幕回放攻击
 *
 * 模型规范:
 * - 输入: [1, 80, 80, 3] NHWC Float32 (BGR 通道顺序, 像素值 0-255)
 * - 输出: [1, 3] Float32 Logits
 * - 类别: Index 0 = Fake 2D, Index 1 = Real, Index 2 = Fake 3D
 */
public class MiniVisionLivenessDetector {
    private static final String TAG = "MiniVision";

    // 模型路径
    private static final String MODEL_PATH = "model/model_float32.tflite";

    // 模型输入输出规格
    private static final int INPUT_SIZE = 80;
    private static final int INPUT_CHANNELS = 3;
    private static final int OUTPUT_CLASSES = 3;

    // 活体阈值 (Real 概率 > 0.90 判定为活体)
    private static final float LIVE_THRESHOLD = 0.90f;

    // ML Kit 框放大系数 (官方标准尺度)
    private static final float SCALE_EXPANSION = 2.7f;

    // TFLite 解释器
    private Interpreter interpreter;

    // 输入缓冲区
    private ByteBuffer inputBuffer;

    // 输出缓冲区
    private float[][] outputBuffer;

    // 张量排布缓存 (NHWC 或 NCHW)
    private Boolean isNCHW = null;

    /**
     * 初始化活体检测器
     *
     * @param context 应用上下文
     */
    public MiniVisionLivenessDetector(Context context) {
        long startTime = System.currentTimeMillis();

        try {
            // 加载模型文件
            MappedByteBuffer modelBuffer = loadModelFile(context);

            // 配置解释器选项
            Interpreter.Options options = new Interpreter.Options();
            // MT6762 是 Cortex-A53 架构，设置 2 线程
            options.setNumThreads(2);

            // 创建解释器
            interpreter = new Interpreter(modelBuffer, options);

            // 预分配输入缓冲区 (1 * 80 * 80 * 3 * 4 bytes)
            inputBuffer = ByteBuffer.allocateDirect(1 * INPUT_SIZE * INPUT_SIZE * INPUT_CHANNELS * 4);
            inputBuffer.order(ByteOrder.nativeOrder());

            // 预分配输出缓冲区
            outputBuffer = new float[1][OUTPUT_CLASSES];

            // 探测张量排布
            int[] inputShape = interpreter.getInputTensor(0).shape();
            isNCHW = (inputShape[1] == 3);

            long endTime = System.currentTimeMillis();
            Log.i(TAG, "初始化完成，耗时: " + (endTime - startTime) + "ms, 张量排布: " + (isNCHW ? "NCHW" : "NHWC"));

        } catch (IOException e) {
            Log.e(TAG, "模型加载失败: " + e.getMessage());
            throw new RuntimeException("Failed to load MiniFASNet model", e);
        }
    }

    /**
     * 判断是否为活体
     *
     * @param frameBitmap 原始帧图像
     * @param mlKitBoundingBox ML Kit 检测到的人脸边界框
     * @return true = 活体通过，false = 疑似攻击
     */
    public boolean isLive(Bitmap frameBitmap, Rect mlKitBoundingBox) {
        if (interpreter == null) {
            Log.e(TAG, "解释器未初始化");
            return false;
        }

        if (frameBitmap == null || mlKitBoundingBox == null) {
            return false;
        }

        long startTime = System.currentTimeMillis();

        try {
            // --- 裁剪逻辑：强制正方形 + 动态缩放降级 + 平移防越界 ---
            float faceCenterX = mlKitBoundingBox.exactCenterX();
            float faceCenterY = mlKitBoundingBox.exactCenterY();
            float faceWidth = mlKitBoundingBox.width();
            float faceHeight = mlKitBoundingBox.height();

            int imgW = frameBitmap.getWidth();
            int imgH = frameBitmap.getHeight();

            // 动态缩放降级
            float targetScale = SCALE_EXPANSION;
            float maxScaleX = (float) imgW / faceWidth;
            float maxScaleY = (float) imgH / faceHeight;
            float finalScale = Math.min(targetScale, Math.min(maxScaleX, maxScaleY));

            // 强制完美正方形
            float cropSize = Math.max(faceWidth, faceHeight) * finalScale;

            // 计算初始边界
            int left = (int) (faceCenterX - cropSize / 2.0f);
            int top = (int) (faceCenterY - cropSize / 2.0f);
            int right = (int) (faceCenterX + cropSize / 2.0f);
            int bottom = (int) (faceCenterY + cropSize / 2.0f);

            // 平移防越界
            if (left < 0) {
                right -= left;
                left = 0;
            }
            if (top < 0) {
                bottom -= top;
                top = 0;
            }
            if (right > imgW) {
                left -= (right - imgW);
                right = imgW;
            }
            if (bottom > imgH) {
                top -= (bottom - imgH);
                bottom = imgH;
            }

            // 安全兜底
            left = Math.max(0, left);
            top = Math.max(0, top);
            right = Math.min(imgW, right);
            bottom = Math.min(imgH, bottom);

            int finalCropWidth = right - left;
            int finalCropHeight = bottom - top;

            if (finalCropWidth <= 0 || finalCropHeight <= 0) {
                return false;
            }

            // 裁剪并缩放
            Bitmap croppedBitmap = Bitmap.createBitmap(frameBitmap, left, top, finalCropWidth, finalCropHeight);
            Bitmap scaled80x80 = Bitmap.createScaledBitmap(croppedBitmap, INPUT_SIZE, INPUT_SIZE, true);

            // 预处理：RGB 转 BGR（不归一化）
            preprocessBitmap(scaled80x80);

            // 释放 Bitmap
            if (croppedBitmap != scaled80x80 && !croppedBitmap.isRecycled()) {
                croppedBitmap.recycle();
            }
            if (scaled80x80 != frameBitmap && !scaled80x80.isRecycled()) {
                scaled80x80.recycle();
            }

            // TFLite 推理
            inputBuffer.rewind();
            interpreter.run(inputBuffer, outputBuffer);

            // Softmax 后处理
            float realProbability = softmaxAndGetRealProb(outputBuffer[0]);

            long totalTime = System.currentTimeMillis();
            Log.i(TAG, "Real概率=" + String.format("%.2f", realProbability) + ", 耗时=" + (totalTime - startTime) + "ms");

            return realProbability > LIVE_THRESHOLD;

        } catch (Exception e) {
            Log.e(TAG, "推理异常: " + e.getMessage());
            return false;
        }
    }

    /**
     * 预处理 Bitmap：RGB 转 BGR（不归一化）
     */
    private void preprocessBitmap(Bitmap bitmap) {
        inputBuffer.rewind();

        int[] pixels = new int[INPUT_SIZE * INPUT_SIZE];
        bitmap.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE);

        if (isNCHW) {
            // NCHW 排布：先写满整张图的 B，再写满 G，最后写满 R
            for (int p : pixels) {
                inputBuffer.putFloat((float) (p & 0xFF));
            }
            for (int p : pixels) {
                inputBuffer.putFloat((float) ((p >> 8) & 0xFF));
            }
            for (int p : pixels) {
                inputBuffer.putFloat((float) ((p >> 16) & 0xFF));
            }
        } else {
            // NHWC 排布：交替写入 B, G, R
            for (int p : pixels) {
                inputBuffer.putFloat((float) (p & 0xFF));
                inputBuffer.putFloat((float) ((p >> 8) & 0xFF));
                inputBuffer.putFloat((float) ((p >> 16) & 0xFF));
            }
        }
    }

    /**
     * Softmax 计算并返回 Real 概率
     */
    private float softmaxAndGetRealProb(float[] logits) {
        float maxLogit = Math.max(logits[0], Math.max(logits[1], logits[2]));

        float sumExp = 0;
        float[] exps = new float[3];
        for (int i = 0; i < 3; i++) {
            exps[i] = (float) Math.exp(logits[i] - maxLogit);
            sumExp += exps[i];
        }

        return exps[1] / sumExp;
    }

    /**
     * 加载模型文件
     */
    private MappedByteBuffer loadModelFile(Context context) throws IOException {
        android.content.res.AssetFileDescriptor fileDescriptor = context.getAssets().openFd(MODEL_PATH);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    /**
     * 释放资源
     */
    public void release() {
        if (interpreter != null) {
            interpreter.close();
            interpreter = null;
        }
    }
}

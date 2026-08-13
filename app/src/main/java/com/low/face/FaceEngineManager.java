package com.low.face;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.low.face.utils.SimFaceWrapper;
import com.simprints.biometrics.simface.data.FaceDetection;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 人脸核心调用管理器
 * 负责：初始化、检测、质量判断、对齐、特征提取、录入、识别、比对
 */
public class FaceEngineManager {
    private static final String TAG = "FaceEngine";

    // 质量阈值（与原 Demo 保持一致）
    public static final float QUALITY_THRESHOLD = 0.4f;
    // 匹配阈值（与原 Demo 保持一致）
    public static final double MATCH_THRESHOLD = 0.85;

    private SimFaceWrapper simFaceWrapper;
    private boolean initialized = false;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // 首次检测标志
    private boolean firstDetect = true;

    /**
     * 初始化 SimFace
     * 必须在后台线程调用
     */
    public synchronized void initialize(Context context) {
        if (initialized) {
            Log.w(TAG, "SimFace 已初始化，跳过");
            return;
        }

        long startTime = System.currentTimeMillis();
        Log.i(TAG, "[初始化] 开始初始化 SimFace...");

        try {
            simFaceWrapper = new SimFaceWrapper(context.getApplicationContext());
            simFaceWrapper.initialize();
            initialized = true;

            long endTime = System.currentTimeMillis();
            Log.i(TAG, "[初始化] 完成，耗时: " + (endTime - startTime) + "ms");
        } catch (Exception e) {
            Log.e(TAG, "[初始化] 失败", e);
            throw new RuntimeException("SimFace 初始化失败: " + e.getMessage(), e);
        }
    }

    /**
     * 是否已初始化
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * 检测人脸
     * 必须在后台线程调用
     */
    public List<FaceDetection> detectFaces(Bitmap bitmap) {
        checkInitialized();
        long startTime = System.currentTimeMillis();

        boolean isFirst = firstDetect;
        if (firstDetect) {
            Log.i(TAG, "[检测] ★ 首次人脸检测开始，Bitmap尺寸: " + bitmap.getWidth() + "x" + bitmap.getHeight());
            firstDetect = false;
        }

        try {
            List<FaceDetection> faces = simFaceWrapper.detectFaces(bitmap);
            long endTime = System.currentTimeMillis();
            long cost = endTime - startTime;

            if (isFirst) {
                Log.i(TAG, "[检测] ★ 首次人脸检测完成，耗时: " + cost + "ms, 检测到 " + faces.size() + " 张人脸");
            } else {
                Log.d(TAG, "[检测] 检测到 " + faces.size() + " 张人脸，耗时: " + cost + "ms");
            }

            return faces;
        } catch (Exception e) {
            Log.e(TAG, "[检测] 失败", e);
            throw new RuntimeException("人脸检测失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取人脸质量分数
     */
    public float getFaceQuality(FaceDetection face) {
        checkInitialized();
        return simFaceWrapper.getFaceQuality(face);
    }

    /**
     * 提取特征向量
     * 必须在后台线程调用
     */
    public byte[] getEmbedding(FaceDetection face, Bitmap sourceBitmap) {
        checkInitialized();
        long startTime = System.currentTimeMillis();

        Bitmap alignedFace = null;
        try {
            long endTime;

            // 对齐人脸
            long alignStartTime = System.currentTimeMillis();
            alignedFace = face.alignedFaceImage(sourceBitmap);
            endTime = System.currentTimeMillis();
            long alignCost = endTime - alignStartTime;
            Log.d(TAG, "[对齐] alignedFaceImage 耗时: " + alignCost + "ms");

            // 提取特征（传入已对齐的 Bitmap）
            long embedStartTime = System.currentTimeMillis();
            byte[] embedding = simFaceWrapper.getEmbedding(alignedFace);
            endTime = System.currentTimeMillis();
            long embedCost = endTime - embedStartTime;

            if (embedding != null) {
                Log.d(TAG, "[特征提取] 向量长度: " + embedding.length + ", 耗时: " + embedCost + "ms");
            }

            long totalCost = System.currentTimeMillis() - startTime;
            Log.d(TAG, "[getEmbedding] 总耗时: " + totalCost + "ms (对齐:" + alignCost + "ms + 特征:" + embedCost + "ms)");

            return embedding;
        } catch (Exception e) {
            Log.e(TAG, "[特征提取] 失败", e);
            return null;
        } finally {
            // 回收对齐后的 Bitmap
            if (alignedFace != null && alignedFace != sourceBitmap && !alignedFace.isRecycled()) {
                alignedFace.recycle();
            }
        }
    }

    /**
     * 1:N 识别比对
     * 返回 (索引, 分数) 列表
     */
    public List<IndexScore> identificationScore(byte[] probe, List<byte[]> references) {
        checkInitialized();
        long startTime = System.currentTimeMillis();

        try {
            List<kotlin.Pair<Integer, Double>> scores = simFaceWrapper.identificationScore(probe, references);

            List<IndexScore> result = new ArrayList<>();
            for (kotlin.Pair<Integer, Double> pair : scores) {
                result.add(new IndexScore(pair.getFirst(), pair.getSecond()));
            }

            long endTime = System.currentTimeMillis();
            Log.d(TAG, "[1:N比对] 候选人数: " + references.size() + "，耗时: " + (endTime - startTime) + "ms");

            return result;
        } catch (Exception e) {
            Log.e(TAG, "[1:N比对] 失败", e);
            throw new RuntimeException("1:N 比对失败: " + e.getMessage(), e);
        }
    }

    /**
     * 录入人脸（复用已检测的人脸结果）
     * 用于自动抓取场景，避免重复检测
     * 必须在后台线程调用
     */
    public FaceEnrollResult enrollFromDetectedFace(Bitmap bitmap, FaceDetection face, String userId, String userName) {
        long startTime = System.currentTimeMillis();
        Log.i(TAG, "[录入] 开始（复用检测结果），userId=" + userId + ", userName=" + userName);

        float quality = simFaceWrapper.getFaceQuality(face);
        Log.i(TAG, "[录入] 人脸质量: " + quality);

        // 提取特征（跳过 detectFaces）
        byte[] embedding = getEmbedding(face, bitmap);
        if (embedding == null) {
            return FaceEnrollResult.failure("特征提取失败");
        }

        // 保存到内存存储
        FaceRecord record = new FaceRecord(userId, userName, embedding, quality);
        FaceStore.add(record);

        long endTime = System.currentTimeMillis();
        Log.i(TAG, "[录入] 成功（复用检测结果），总耗时: " + (endTime - startTime) + "ms");

        return FaceEnrollResult.success("录入成功: " + userName, quality);
    }

    /**
     * 录入人脸
     * 必须在后台线程调用
     */
    public FaceEnrollResult enroll(Bitmap bitmap, String userId, String userName) {
        long startTime = System.currentTimeMillis();
        Log.i(TAG, "[录入] 开始，userId=" + userId + ", userName=" + userName);

        // 检测人脸
        List<FaceDetection> faces = detectFaces(bitmap);

        if (faces.isEmpty()) {
            return FaceEnrollResult.failure("未检测到人脸");
        }

        if (faces.size() > 1) {
            Log.w(TAG, "[录入] 检测到多张人脸，只使用第一张");
        }

        FaceDetection face = faces.get(0);
        float quality = simFaceWrapper.getFaceQuality(face);
        Log.i(TAG, "[录入] 人脸质量: " + quality);

        if (quality < QUALITY_THRESHOLD) {
            return FaceEnrollResult.failure("人脸质量不足 (" + String.format("%.2f", quality) + " < " + QUALITY_THRESHOLD + ")");
        }

        // 提取特征
        byte[] embedding = getEmbedding(face, bitmap);
        if (embedding == null) {
            return FaceEnrollResult.failure("特征提取失败");
        }

        // 保存到内存存储
        FaceRecord record = new FaceRecord(userId, userName, embedding, quality);
        FaceStore.add(record);

        long endTime = System.currentTimeMillis();
        Log.i(TAG, "[录入] 成功，总耗时: " + (endTime - startTime) + "ms");

        return FaceEnrollResult.success("录入成功: " + userName, quality);
    }

    /**
     * 识别人脸（复用已检测的人脸结果）
     * 用于自动抓取场景，避免重复检测
     * 必须在后台线程调用
     */
    public FaceRecognitionResult recognizeFromDetectedFace(Bitmap bitmap, FaceDetection face) {
        long startTime = System.currentTimeMillis();
        Log.i(TAG, "[识别] 开始（复用检测结果），已录入人数: " + FaceStore.count());

        if (FaceStore.count() == 0) {
            return FaceRecognitionResult.failure("请先录入人脸");
        }

        float quality = simFaceWrapper.getFaceQuality(face);

        // 提取特征（跳过 detectFaces）
        byte[] probeEmbedding = getEmbedding(face, bitmap);
        if (probeEmbedding == null) {
            return FaceRecognitionResult.failure("特征提取失败");
        }

        // 1:N 比对
        List<byte[]> allEmbeddings = FaceStore.getAllEmbeddings();
        List<IndexScore> scores = identificationScore(probeEmbedding, allEmbeddings);

        // 找最高分
        IndexScore bestMatch = null;
        for (IndexScore score : scores) {
            if (bestMatch == null || score.score > bestMatch.score) {
                bestMatch = score;
            }
        }

        long endTime = System.currentTimeMillis();
        Log.i(TAG, "[识别] 总耗时（复用检测结果）: " + (endTime - startTime) + "ms");

        if (bestMatch != null && bestMatch.score >= MATCH_THRESHOLD) {
            FaceRecord record = FaceStore.get(bestMatch.index);
            if (record != null) {
                Log.i(TAG, "[识别] 匹配成功: " + record.userName + ", score=" + bestMatch.score);
                return FaceRecognitionResult.matched(record.userId, record.userName, bestMatch.score);
            }
        }

        double bestScore = bestMatch != null ? bestMatch.score : 0;
        Log.i(TAG, "[识别] 未找到匹配，最高分数: " + bestScore);
        return FaceRecognitionResult.notMatched(bestScore);
    }

    /**
     * 识别人脸
     * 必须在后台线程调用
     */
    public FaceRecognitionResult recognize(Bitmap bitmap) {
        long startTime = System.currentTimeMillis();
        Log.i(TAG, "[识别] 开始，已录入人数: " + FaceStore.count());

        if (FaceStore.count() == 0) {
            return FaceRecognitionResult.failure("请先录入人脸");
        }

        // 检测人脸
        List<FaceDetection> faces = detectFaces(bitmap);

        if (faces.isEmpty()) {
            return FaceRecognitionResult.failure("未检测到人脸");
        }

        if (faces.size() > 1) {
            Log.w(TAG, "[识别] 检测到多张人脸，只使用第一张");
        }

        FaceDetection face = faces.get(0);
        float quality = simFaceWrapper.getFaceQuality(face);

        if (quality < QUALITY_THRESHOLD) {
            return FaceRecognitionResult.failure("人脸质量不足，请调整后重试");
        }

        // 提取特征
        byte[] probeEmbedding = getEmbedding(face, bitmap);
        if (probeEmbedding == null) {
            return FaceRecognitionResult.failure("特征提取失败");
        }

        // 1:N 比对
        List<byte[]> allEmbeddings = FaceStore.getAllEmbeddings();
        List<IndexScore> scores = identificationScore(probeEmbedding, allEmbeddings);

        // 找最高分
        IndexScore bestMatch = null;
        for (IndexScore score : scores) {
            if (bestMatch == null || score.score > bestMatch.score) {
                bestMatch = score;
            }
        }

        long endTime = System.currentTimeMillis();
        Log.i(TAG, "[识别] 总耗时: " + (endTime - startTime) + "ms");

        if (bestMatch != null && bestMatch.score >= MATCH_THRESHOLD) {
            FaceRecord record = FaceStore.get(bestMatch.index);
            if (record != null) {
                Log.i(TAG, "[识别] 匹配成功: " + record.userName + ", score=" + bestMatch.score);
                return FaceRecognitionResult.matched(record.userId, record.userName, bestMatch.score);
            }
        }

        double bestScore = bestMatch != null ? bestMatch.score : 0;
        Log.i(TAG, "[识别] 未找到匹配，最高分数: " + bestScore);
        return FaceRecognitionResult.notMatched(bestScore);
    }

    /**
     * 释放资源
     */
    public synchronized void release() {
        if (simFaceWrapper != null && initialized) {
            try {
                simFaceWrapper.release();
                Log.i(TAG, "[释放] SimFace 资源已释放");
            } catch (Exception e) {
                Log.e(TAG, "[释放] 失败", e);
            }
            initialized = false;
        }
        executor.shutdown();
    }

    private void checkInitialized() {
        if (!initialized || simFaceWrapper == null) {
            throw new IllegalStateException("SimFace 未初始化，请先调用 initialize()");
        }
    }

    /**
     * 获取执行器
     */
    public ExecutorService getExecutor() {
        return executor;
    }

    /**
     * 简单的 Pair 类
     */
    public static class Pair<F, S> {
        public final F first;
        public final S second;

        public Pair(F first, S second) {
            this.first = first;
            this.second = second;
        }
    }

    /**
     * 索引和分数
     */
    public static class IndexScore {
        public final int index;
        public final double score;

        public IndexScore(int index, double score) {
            this.index = index;
            this.score = score;
        }
    }
}

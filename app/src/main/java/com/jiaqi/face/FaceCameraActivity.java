package com.jiaqi.face;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;
import com.simprints.biometrics.simface.data.FaceDetection;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 相机页面 - 实时人脸检测，质量达标自动抓取
 */
public class FaceCameraActivity extends AppCompatActivity {
    private static final String TAG = "FaceCamera";
    private static final int REQUEST_CAMERA_PERMISSION = 1001;
    private static final int ANALYSIS_IMAGE_MAX_WIDTH = 500;

    // UI 控件
    private PreviewView previewView;
    private OverlayView overlayView;
    private TextView tvStatus;
    private TextView tvQuality;
    private Button btnCancel;
    private ProgressBar progressBar;

    // CameraX
    private ProcessCameraProvider cameraProvider;
    private ExecutorService cameraExecutor;

    // 引擎管理器
    private FaceEngineManager engineManager;

    // 当前模式
    private int currentMode;
    private static final int MODE_ENROLL = 1;
    private static final int MODE_RECOGNIZE = 2;

    // 从 Intent 传递的工号和姓名
    private String workId;
    private String userName;

    // 是否已处理
    private boolean hasProcessed = false;

    // 帧计数器
    private int frameCount = 0;

    // 性能日志 - 记录各阶段时间
    private long activityCreateTime;    // Activity 创建时间
    private long cameraInitStartTime;   // 相机初始化开始时间
    private long firstFrameTime;        // 第一帧到达时间
    private boolean firstFrameLogged = false;  // 是否已记录第一帧

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        activityCreateTime = System.currentTimeMillis();
        Log.i(TAG, "======== 相机页面开始 ========");
        Log.i(TAG, "[性能] onCreate 开始: " + activityCreateTime);

        super.onCreate(savedInstanceState);

        long layoutStartTime = System.currentTimeMillis();
        setContentView(R.layout.activity_face_camera);
        Log.i(TAG, "[性能] setContentView 耗时: " + (System.currentTimeMillis() - layoutStartTime) + "ms");

        initViews();
        getIntentData();

        // 检查相机权限
        long permCheckTime = System.currentTimeMillis();
        if (hasCameraPermission()) {
            Log.i(TAG, "[性能] 权限检查耗时: " + (System.currentTimeMillis() - permCheckTime) + "ms (已有权限)");
            initCamera();
        } else {
            Log.i(TAG, "[性能] 权限检查耗时: " + (System.currentTimeMillis() - permCheckTime) + "ms (需请求权限)");
            requestCameraPermission();
        }

        Log.i(TAG, "[性能] onCreate 总耗时: " + (System.currentTimeMillis() - activityCreateTime) + "ms");
    }

    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this,
            new String[]{Manifest.permission.CAMERA},
            REQUEST_CAMERA_PERMISSION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initCamera();
            } else {
                Toast.makeText(this, "需要相机权限", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void initViews() {
        long startTime = System.currentTimeMillis();

        previewView = findViewById(R.id.previewView);
        overlayView = findViewById(R.id.overlayView);
        tvStatus = findViewById(R.id.tvStatus);
        tvQuality = findViewById(R.id.tvQuality);
        btnCancel = findViewById(R.id.btnCancel);
        progressBar = findViewById(R.id.progressBar);

        // 获取引擎管理器
        engineManager = FaceEngineSingleton.getInstance();

        btnCancel.setOnClickListener(v -> finish());

        Log.i(TAG, "[性能] initViews 耗时: " + (System.currentTimeMillis() - startTime) + "ms");
    }

    private void getIntentData() {
        long startTime = System.currentTimeMillis();

        currentMode = getIntent().getIntExtra("mode", MODE_ENROLL);
        workId = getIntent().getStringExtra("workId");
        userName = getIntent().getStringExtra("userName");

        if (workId == null || workId.isEmpty()) workId = "default_id";
        if (userName == null || userName.isEmpty()) userName = "default_name";

        tvStatus.setText(currentMode == MODE_ENROLL ? "正在录入..." : "正在识别...");

        Log.i(TAG, "[性能] getIntentData 耗时: " + (System.currentTimeMillis() - startTime) + "ms");
    }

    private void initCamera() {
        cameraInitStartTime = System.currentTimeMillis();
        Log.i(TAG, "[性能] ===== 开始相机初始化 =====");

        long executorCreateTime = System.currentTimeMillis();
        cameraExecutor = Executors.newSingleThreadExecutor();
        Log.i(TAG, "[性能] 创建 Executor 耗时: " + (System.currentTimeMillis() - executorCreateTime) + "ms");

        long providerStartTime = System.currentTimeMillis();
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
            ProcessCameraProvider.getInstance(this);
        Log.i(TAG, "[性能] ProcessCameraProvider.getInstance 耗时: " + (System.currentTimeMillis() - providerStartTime) + "ms (异步)");

        cameraProviderFuture.addListener(() -> {
            long listenerTriggerTime = System.currentTimeMillis();
            Log.i(TAG, "[性能] Listener 触发，耗时: " + (listenerTriggerTime - cameraInitStartTime) + "ms (从initCamera开始)");

            try {
                long getProviderTime = System.currentTimeMillis();
                cameraProvider = cameraProviderFuture.get();
                Log.i(TAG, "[性能] cameraProviderFuture.get() 耗时: " + (System.currentTimeMillis() - getProviderTime) + "ms");

                bindCameraUseCases();

                long totalTime = System.currentTimeMillis() - cameraInitStartTime;
                Log.i(TAG, "[性能] ===== 相机初始化完成 ===== 总耗时: " + totalTime + "ms");

            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "[相机] 初始化失败", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "相机初始化失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraUseCases() {
        long bindStartTime = System.currentTimeMillis();
        Log.i(TAG, "[性能] ----- bindCameraUseCases 开始 -----");

        // 预览
        long previewCreateTime = System.currentTimeMillis();
        Preview preview = new Preview.Builder().build();
        Log.i(TAG, "[性能] Preview.Builder 耗时: " + (System.currentTimeMillis() - previewCreateTime) + "ms");

        long surfaceProviderTime = System.currentTimeMillis();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());
        Log.i(TAG, "[性能] setSurfaceProvider 耗时: " + (System.currentTimeMillis() - surfaceProviderTime) + "ms");

        // 图像分析 - 实时检测人脸
        long analysisCreateTime = System.currentTimeMillis();
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
            .build();
        Log.i(TAG, "[性能] ImageAnalysis.Builder 耗时: " + (System.currentTimeMillis() - analysisCreateTime) + "ms");

        imageAnalysis.setAnalyzer(cameraExecutor, new ImageAnalysis.Analyzer() {
            @Override
            public void analyze(@NonNull ImageProxy imageProxy) {
                // 记录第一帧时间
                if (!firstFrameLogged) {
                    firstFrameTime = System.currentTimeMillis();
                    firstFrameLogged = true;
                    Log.i(TAG, "[性能] ★★★ 第一帧到达 ★★★ 距onCreate: " + (firstFrameTime - activityCreateTime) + "ms");
                    Log.i(TAG, "[性能] 第一帧图像尺寸: " + imageProxy.getWidth() + "x" + imageProxy.getHeight() +
                        ", rotation: " + imageProxy.getImageInfo().getRotationDegrees());
                }

                if (hasProcessed) {
                    imageProxy.close();
                    return;
                }

                try {
                    frameCount++;

                    // 每30帧打印一次日志
                    if (frameCount % 30 == 0) {
                        Log.d(TAG, "[帧] 第" + frameCount + "帧");
                    }

                    long frameStartTime = System.currentTimeMillis();

                    // 转换为 Bitmap
                    long bitmapStartTime = System.currentTimeMillis();
                    Bitmap bitmap = imageProxyToBitmap(imageProxy);
                    long bitmapEndTime = System.currentTimeMillis();

                    if (frameCount <= 5) {
                        Log.i(TAG, "[性能] 第" + frameCount + "帧 Bitmap转换耗时: " + (bitmapEndTime - bitmapStartTime) + "ms");
                    }

                    if (bitmap == null) {
                        imageProxy.close();
                        return;
                    }

                    // 缩放
                    long resizeStartTime = System.currentTimeMillis();
                    final Bitmap resizedBitmap = resizeBitmap(bitmap);
                    long resizeEndTime = System.currentTimeMillis();

                    if (frameCount <= 5) {
                        Log.i(TAG, "[性能] 第" + frameCount + "帧 resizeBitmap耗时: " + (resizeEndTime - resizeStartTime) + "ms" +
                            ", 尺寸: " + resizedBitmap.getWidth() + "x" + resizedBitmap.getHeight());
                    }

                    // 检测人脸
                    long detectStartTime = System.currentTimeMillis();
                    List<FaceDetection> faces = engineManager.detectFaces(resizedBitmap);
                    long detectEndTime = System.currentTimeMillis();

                    if (frameCount <= 5) {
                        Log.i(TAG, "[性能] 第" + frameCount + "帧 detectFaces耗时: " + (detectEndTime - detectStartTime) + "ms" +
                            ", 检测到 " + faces.size() + " 张人脸");
                    }

                    long frameEndTime = System.currentTimeMillis();
                    if (frameCount <= 5) {
                        Log.i(TAG, "[性能] 第" + frameCount + "帧 总处理耗时: " + (frameEndTime - frameStartTime) + "ms");
                    }

                    if (!faces.isEmpty()) {
                        final FaceDetection face = faces.get(0);
                        final float quality = engineManager.getFaceQuality(face);

                        // 更新 UI
                        long uiStartTime = System.currentTimeMillis();
                        runOnUiThread(() -> {
                            overlayView.setFaceRect(face.getAbsoluteBoundingBox(), quality,
                                previewView.getWidth(), previewView.getHeight(),
                                resizedBitmap.getWidth(), resizedBitmap.getHeight());
                            tvQuality.setText("质量: " + String.format("%.2f", quality));

                            if (quality >= FaceEngineManager.QUALITY_THRESHOLD) {
                                tvQuality.setTextColor(android.graphics.Color.parseColor("#00C853"));
                                tvStatus.setText("✅ 质量达标，正在处理...");
                            } else {
                                tvQuality.setTextColor(android.graphics.Color.parseColor("#FFEB3B"));
                                tvStatus.setText("⚠ 请调整角度");
                            }
                        });

                        // 质量达标时自动处理
                        if (quality >= FaceEngineManager.QUALITY_THRESHOLD && !hasProcessed) {
                            hasProcessed = true;
                            Log.i(TAG, "[性能] 自动抓取! quality=" + quality + ", 第" + frameCount + "帧");
                            processImage(resizedBitmap);
                        }
                    } else {
                        runOnUiThread(() -> {
                            overlayView.clearFaceRect();
                            tvQuality.setText("未检测到人脸");
                            tvQuality.setTextColor(android.graphics.Color.parseColor("#FF1744"));
                            tvStatus.setText("请将人脸对准框内");
                        });
                    }

                } catch (Exception e) {
                    Log.e(TAG, "[分析] 异常", e);
                } finally {
                    imageProxy.close();
                }
            }
        });

        // 前置摄像头
        CameraSelector cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;

        try {
            long bindLifecycleStartTime = System.currentTimeMillis();
            cameraProvider.unbindAll();
            cameraProvider.bindToLifecycle(
                (LifecycleOwner) this,
                cameraSelector,
                preview,
                imageAnalysis
            );
            Log.i(TAG, "[性能] bindToLifecycle 耗时: " + (System.currentTimeMillis() - bindLifecycleStartTime) + "ms");
            Log.i(TAG, "[性能] ----- bindCameraUseCases 完成 ----- 总耗时: " + (System.currentTimeMillis() - bindStartTime) + "ms");
        } catch (Exception e) {
            Log.e(TAG, "[相机] 绑定失败", e);
        }
    }

    /**
     * 处理抓取的图像
     */
    private void processImage(Bitmap bitmap) {
        runOnUiThread(() -> progressBar.setVisibility(View.VISIBLE));

        engineManager.getExecutor().execute(() -> {
            long startTime = System.currentTimeMillis();

            if (currentMode == MODE_ENROLL) {
                FaceEnrollResult result = engineManager.enroll(bitmap, workId, userName);
                long endTime = System.currentTimeMillis();
                Log.i(TAG, "[性能] enroll 总耗时: " + (endTime - startTime) + "ms");

                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    returnResult(result.success, result.message);
                });
            } else {
                FaceRecognitionResult result = engineManager.recognize(bitmap);
                long endTime = System.currentTimeMillis();
                Log.i(TAG, "[性能] recognize 总耗时: " + (endTime - startTime) + "ms");

                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    returnResult(result.matched, result.message);
                });
            }
        });
    }

    /**
     * ImageProxy 转 Bitmap
     */
    private Bitmap imageProxyToBitmap(ImageProxy imageProxy) {
        try {
            long startTime = System.currentTimeMillis();

            ImageProxy.PlaneProxy[] planes = imageProxy.getPlanes();
            ByteBuffer yBuffer = planes[0].getBuffer();
            ByteBuffer uBuffer = planes[1].getBuffer();
            ByteBuffer vBuffer = planes[2].getBuffer();

            int ySize = yBuffer.remaining();
            int uSize = uBuffer.remaining();
            int vSize = vBuffer.remaining();

            byte[] nv21 = new byte[ySize + uSize + vSize];

            yBuffer.get(nv21, 0, ySize);
            vBuffer.get(nv21, ySize, vSize);
            uBuffer.get(nv21, ySize + vSize, uSize);

            long nv21Time = System.currentTimeMillis();
            if (frameCount <= 3) {
                Log.d(TAG, "[Bitmap] NV21组装耗时: " + (nv21Time - startTime) + "ms");
            }

            YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21,
                imageProxy.getWidth(), imageProxy.getHeight(), null);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            yuvImage.compressToJpeg(new Rect(0, 0,
                imageProxy.getWidth(), imageProxy.getHeight()), 100, out);
            byte[] jpegBytes = out.toByteArray();

            long jpegTime = System.currentTimeMillis();
            if (frameCount <= 3) {
                Log.d(TAG, "[Bitmap] JPEG压缩耗时: " + (jpegTime - nv21Time) + "ms");
            }

            Bitmap bitmap = BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.length);

            long decodeTime = System.currentTimeMillis();
            if (frameCount <= 3) {
                Log.d(TAG, "[Bitmap] JPEG解码耗时: " + (decodeTime - jpegTime) + "ms");
            }

            // 旋转
            int rotation = imageProxy.getImageInfo().getRotationDegrees();
            if (rotation != 0 && bitmap != null) {
                long rotateStartTime = System.currentTimeMillis();
                Matrix matrix = new Matrix();
                matrix.postRotate(rotation);
                bitmap = Bitmap.createBitmap(bitmap, 0, 0,
                    bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                if (frameCount <= 3) {
                    Log.d(TAG, "[Bitmap] 旋转耗时: " + (System.currentTimeMillis() - rotateStartTime) + "ms, rotation=" + rotation);
                }
            }

            // 前置摄像头镜像
            if (bitmap != null) {
                long mirrorStartTime = System.currentTimeMillis();
                Matrix matrix = new Matrix();
                matrix.preScale(-1, 1);
                bitmap = Bitmap.createBitmap(bitmap, 0, 0,
                    bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                if (frameCount <= 3) {
                    Log.d(TAG, "[Bitmap] 镜像耗时: " + (System.currentTimeMillis() - mirrorStartTime) + "ms");
                }
            }

            long totalBitmapTime = System.currentTimeMillis() - startTime;
            if (frameCount <= 3) {
                Log.i(TAG, "[Bitmap] imageProxyToBitmap 总耗时: " + totalBitmapTime + "ms");
            }

            return bitmap;
        } catch (Exception e) {
            Log.e(TAG, "[Bitmap转换] 失败", e);
            return null;
        }
    }

    /**
     * 缩放 Bitmap
     */
    private Bitmap resizeBitmap(Bitmap bitmap) {
        int width = bitmap.getWidth();
        if (width <= ANALYSIS_IMAGE_MAX_WIDTH) {
            return bitmap;
        }

        float aspectRatio = (float) bitmap.getHeight() / width;
        int newWidth = ANALYSIS_IMAGE_MAX_WIDTH;
        int newHeight = (int) (newWidth * aspectRatio);

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
    }

    /**
     * 返回结果
     */
    private void returnResult(boolean success, String result) {
        Intent intent = new Intent();
        intent.putExtra("success", success);
        intent.putExtra("result", result);
        intent.putExtra("mode", currentMode);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        Log.i(TAG, "[性能] Activity onDestroy");
        super.onDestroy();
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
    }
}

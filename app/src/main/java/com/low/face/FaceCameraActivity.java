package com.low.face;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Rect;
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

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

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

    // MiniVision 深度学习活体检测器
    private MiniVisionLivenessDetector miniVisionDetector;

    // 当前模式
    private int currentMode;
    private static final int MODE_ENROLL = 1;
    private static final int MODE_RECOGNIZE = 2;

    // 从 Intent 传递的工号和姓名
    private String workId;
    private String userName;

    // 是否已处理
    private boolean hasProcessed = false;

    // 检测节流
    private final AtomicBoolean isDetecting = new AtomicBoolean(false);
    private long lastDetectStartTime = 0L;
    private static final long DETECT_INTERVAL = 800L;

    // 帧计数器
    private int frameCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_camera);

        initViews();
        getIntentData();

        if (hasCameraPermission()) {
            initCamera();
        } else {
            requestCameraPermission();
        }
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
        previewView = findViewById(R.id.previewView);
        overlayView = findViewById(R.id.overlayView);
        tvStatus = findViewById(R.id.tvStatus);
        tvQuality = findViewById(R.id.tvQuality);
        btnCancel = findViewById(R.id.btnCancel);
        progressBar = findViewById(R.id.progressBar);

        engineManager = FaceEngineSingleton.getInstance();
        miniVisionDetector = new MiniVisionLivenessDetector(this);
        btnCancel.setOnClickListener(v -> finish());
    }

    private void getIntentData() {
        currentMode = getIntent().getIntExtra("mode", MODE_ENROLL);
        workId = getIntent().getStringExtra("workId");
        userName = getIntent().getStringExtra("userName");

        if (workId == null || workId.isEmpty()) workId = "default_id";
        if (userName == null || userName.isEmpty()) userName = "default_name";

        tvStatus.setText(currentMode == MODE_ENROLL ? "正在录入..." : "正在识别...");
    }

    private void initCamera() {
        cameraExecutor = Executors.newSingleThreadExecutor();

        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
            ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases();
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "相机初始化失败", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "相机初始化失败", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraUseCases() {
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build();

        imageAnalysis.setAnalyzer(cameraExecutor, imageProxy -> {
            if (hasProcessed) {
                imageProxy.close();
                return;
            }

            long now = System.currentTimeMillis();
            if (now - lastDetectStartTime < DETECT_INTERVAL) {
                imageProxy.close();
                return;
            }

            if (!isDetecting.compareAndSet(false, true)) {
                imageProxy.close();
                return;
            }

            lastDetectStartTime = now;
            frameCount++;

            try {
                Bitmap bitmap = rgbaImageProxyToBitmap(imageProxy);
                imageProxy.close();

                if (bitmap == null) {
                    isDetecting.set(false);
                    return;
                }

                final Bitmap resizedBitmap = resizeBitmap(bitmap);
                List<FaceDetection> faces = engineManager.detectFaces(resizedBitmap);

                if (!faces.isEmpty()) {
                    final FaceDetection face = faces.get(0);
                    final float quality = engineManager.getFaceQuality(face);

                    // 姿态角检查
                    final float yaw = face.getYaw();
                    final float roll = face.getRoll();
                    final float MAX_YAW = 30.0f;
                    final float MAX_ROLL = 20.0f;
                    boolean isPoseValid = Math.abs(yaw) <= MAX_YAW && Math.abs(roll) <= MAX_ROLL;

                    // 活体检测
                    final boolean isPoseValidFinal = isPoseValid;
                    final boolean isLiveByModel;

                    if (isPoseValid) {
                        isLiveByModel = miniVisionDetector.isLive(resizedBitmap, face.getAbsoluteBoundingBox());
                    } else {
                        isLiveByModel = false;
                    }

                    // 更新 UI
                    runOnUiThread(() -> {
                        overlayView.setFaceRect(face.getAbsoluteBoundingBox(), quality,
                            previewView.getWidth(), previewView.getHeight(),
                            resizedBitmap.getWidth(), resizedBitmap.getHeight(), true);

                        tvQuality.setText("质量: " + String.format("%.2f", quality));

                        if (!isPoseValidFinal) {
                            tvQuality.setTextColor(android.graphics.Color.parseColor("#FF9100"));
                            tvStatus.setText("请正对镜头");
                            return;
                        }

                        if (!isLiveByModel) {
                            tvQuality.setTextColor(android.graphics.Color.parseColor("#FF1744"));
                            tvStatus.setText("未检测到活体，请勿使用照片");
                            return;
                        }

                        if (quality >= FaceEngineManager.QUALITY_THRESHOLD) {
                            tvQuality.setTextColor(android.graphics.Color.parseColor("#00C853"));
                            tvStatus.setText("活体通过，正在处理...");
                        } else {
                            tvQuality.setTextColor(android.graphics.Color.parseColor("#FFEB3B"));
                            tvStatus.setText("活体通过，请调整角度");
                        }
                    });

                    // 处理
                    if (isPoseValid && isLiveByModel
                        && quality >= FaceEngineManager.QUALITY_THRESHOLD && !hasProcessed) {
                        hasProcessed = true;
                        Log.i(TAG, "活体通过，开始" + (currentMode == MODE_ENROLL ? "录入" : "识别"));
                        processImageWithDetectedFace(resizedBitmap, face);
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
                Log.e(TAG, "检测异常", e);
            } finally {
                isDetecting.set(false);
            }
        });

        CameraSelector cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;

        try {
            cameraProvider.unbindAll();
            cameraProvider.bindToLifecycle(
                (LifecycleOwner) this,
                cameraSelector,
                preview,
                imageAnalysis
            );
        } catch (Exception e) {
            Log.e(TAG, "相机绑定失败", e);
        }
    }

    /**
     * 处理抓取的图像（复用已检测的人脸结果）
     */
    private void processImageWithDetectedFace(Bitmap bitmap, FaceDetection face) {
        runOnUiThread(() -> progressBar.setVisibility(View.VISIBLE));

        engineManager.getExecutor().execute(() -> {
            if (currentMode == MODE_ENROLL) {
                FaceEnrollResult result = engineManager.enrollFromDetectedFace(bitmap, face, workId, userName);
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    returnResult(result.success, result.message);
                });
            } else {
                FaceRecognitionResult result = engineManager.recognizeFromDetectedFace(bitmap, face);
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    returnResult(result.matched, result.message);
                });
            }
        });
    }

    /**
     * ImageProxy 转 Bitmap（RGBA_8888 格式直接转换）
     */
    private Bitmap rgbaImageProxyToBitmap(ImageProxy imageProxy) {
        try {
            ImageProxy.PlaneProxy plane = imageProxy.getPlanes()[0];
            ByteBuffer buffer = plane.getBuffer();

            int width = imageProxy.getWidth();
            int height = imageProxy.getHeight();
            int rowStride = plane.getRowStride();
            int pixelStride = plane.getPixelStride();

            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

            if (rowStride == width * 4 && pixelStride == 4) {
                buffer.rewind();
                bitmap.copyPixelsFromBuffer(buffer);
            } else {
                int[] pixels = new int[width * height];
                buffer.rewind();

                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        int index = y * rowStride + x * pixelStride;
                        int r = buffer.get(index) & 0xFF;
                        int g = buffer.get(index + 1) & 0xFF;
                        int b = buffer.get(index + 2) & 0xFF;
                        int a = buffer.get(index + 3) & 0xFF;
                        pixels[y * width + x] = (a << 24) | (r << 16) | (g << 8) | b;
                    }
                }
                bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
            }

            // 旋转
            int rotation = imageProxy.getImageInfo().getRotationDegrees();
            if (rotation != 0 && bitmap != null) {
                Matrix matrix = new Matrix();
                matrix.postRotate(rotation);
                Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0,
                    bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                if (rotatedBitmap != bitmap) {
                    bitmap.recycle();
                }
                bitmap = rotatedBitmap;
            }

            return bitmap;
        } catch (Exception e) {
            Log.e(TAG, "Bitmap转换失败", e);
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
        super.onDestroy();

        if (miniVisionDetector != null) {
            miniVisionDetector.release();
        }

        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
    }
}

package com.low.face;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/**
 * 人脸识别 Demo 主页面
 */
public class FaceDemoActivity extends AppCompatActivity {
    private static final String TAG = "FaceDemo";
    private static final int REQUEST_CODE_CAMERA = 1001;
    private static final int MODE_ENROLL = 1;
    private static final int MODE_RECOGNIZE = 2;

    // UI 控件
    private TextView tvSdkStatus;
    private TextView tvEnrolledCount;
    private EditText etWorkId;
    private EditText etName;
    private Button btnEnroll;
    private Button btnRecognize;
    private Button btnClear;
    private ProgressBar progressBar;
    private TextView tvStatus;
    private TextView tvResult;

    // 引擎管理器（使用单例）
    private FaceEngineManager engineManager;

    // 当前模式
    private int currentMode = MODE_ENROLL;

    // 性能日志
    private long appOpenTime;
    private long workIdFocusTime;
    private long nameFocusTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        appOpenTime = System.currentTimeMillis();
        Log.i(TAG, "[App] 打开时间: " + appOpenTime);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_demo);

        initViews();
        initListeners();
        initEngineAsync();
    }

    private void initViews() {
        tvSdkStatus = findViewById(R.id.tvSdkStatus);
        tvEnrolledCount = findViewById(R.id.tvEnrolledCount);
        etWorkId = findViewById(R.id.etWorkId);
        etName = findViewById(R.id.etName);
        btnEnroll = findViewById(R.id.btnEnroll);
        btnRecognize = findViewById(R.id.btnRecognize);
        btnClear = findViewById(R.id.btnClear);
        progressBar = findViewById(R.id.progressBar);
        tvStatus = findViewById(R.id.tvStatus);
        tvResult = findViewById(R.id.tvResult);

        // 更新录入人数
        updateEnrolledCount();
    }

    private void initListeners() {
        // 工号输入框焦点监听
        etWorkId.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && workIdFocusTime == 0) {
                workIdFocusTime = System.currentTimeMillis();
                Log.i(TAG, "[性能] 工号输入框首次获得焦点，耗时: " + (workIdFocusTime - appOpenTime) + "ms");
            }
        });

        // 姓名输入框焦点监听
        etName.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && nameFocusTime == 0) {
                nameFocusTime = System.currentTimeMillis();
                Log.i(TAG, "[性能] 姓名输入框首次获得焦点，耗时: " + (nameFocusTime - appOpenTime) + "ms");
            }
        });

        // 工号输入框文本变化监听
        etWorkId.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(android.text.Editable s) {
                Log.d(TAG, "[输入] 工号文本变化: " + s.toString());
            }
        });

        // 姓名输入框文本变化监听
        etName.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(android.text.Editable s) {
                Log.d(TAG, "[输入] 姓名文本变化: " + s.toString());
            }
        });

        // 录入按钮
        btnEnroll.setOnClickListener(v -> {
            String workId = etWorkId.getText().toString().trim();
            String name = etName.getText().toString().trim();

            if (workId.isEmpty()) {
                Toast.makeText(this, "请输入工号", Toast.LENGTH_SHORT).show();
                return;
            }

            if (name.isEmpty()) {
                Toast.makeText(this, "请输入姓名", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!engineManager.isInitialized()) {
                Toast.makeText(this, "SDK 正在初始化，请稍候...", Toast.LENGTH_SHORT).show();
                return;
            }

            currentMode = MODE_ENROLL;
            openCamera();
        });

        // 识别按钮
        btnRecognize.setOnClickListener(v -> {
            if (FaceStore.count() == 0) {
                Toast.makeText(this, "请先录入人脸", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!engineManager.isInitialized()) {
                Toast.makeText(this, "SDK 正在初始化，请稍候...", Toast.LENGTH_SHORT).show();
                return;
            }

            currentMode = MODE_RECOGNIZE;
            openCamera();
        });

        // 清空按钮
        btnClear.setOnClickListener(v -> {
            FaceStore.clear();
            updateEnrolledCount();
            tvResult.setText("已清空所有录入数据");
            Toast.makeText(this, "已清空", Toast.LENGTH_SHORT).show();
            Log.i(TAG, "[清空] 已清空所有录入数据");
        });
    }

    private void initEngineAsync() {
        engineManager = FaceEngineSingleton.getInstance();
        engineManager.getExecutor().execute(() -> {
            try {
                engineManager.initialize(getApplicationContext());
                runOnUiThread(() -> {
                    tvSdkStatus.setText("SDK状态：已就绪");
                    long initTime = System.currentTimeMillis();
                    Log.i(TAG, "[性能] SDK 初始化完成，总耗时: " + (initTime - appOpenTime) + "ms");
                });
            } catch (Exception e) {
                Log.e(TAG, "[初始化] SDK 初始化失败", e);
                runOnUiThread(() -> {
                    tvSdkStatus.setText("SDK状态：初始化失败");
                    tvResult.setText("SDK 初始化失败: " + e.getMessage());
                });
            }
        });
    }

    private void openCamera() {
        Intent intent = new Intent(this, FaceCameraActivity.class);
        intent.putExtra("mode", currentMode);
        // 录入模式需要传递工号和姓名
        if (currentMode == MODE_ENROLL) {
            intent.putExtra("workId", etWorkId.getText().toString().trim());
            intent.putExtra("userName", etName.getText().toString().trim());
        }
        startActivityForResult(intent, REQUEST_CODE_CAMERA);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_CAMERA && resultCode == RESULT_OK && data != null) {
            // 获取相机返回的结果
            String result = data.getStringExtra("result");
            boolean success = data.getBooleanExtra("success", false);
            int mode = data.getIntExtra("mode", MODE_ENROLL);

            if (mode == MODE_ENROLL) {
                // 录入结果
                updateEnrolledCount();
                tvResult.setText("录入结果: " + result);
            } else {
                // 识别结果
                tvResult.setText("识别结果: " + result);
            }

            tvStatus.setText(success ? "处理完成" : "处理失败");
        }
    }

    private void updateEnrolledCount() {
        tvEnrolledCount.setText("已录入人数: " + FaceStore.count());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 不在这里释放，因为使用单例，生命周期跟随 Application
    }
}

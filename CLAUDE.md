# CLAUDE.md

本项目是一个轻量级人脸识别 Demo，使用原生 XML/View 实现，适配低端设备。

## 项目结构

```
lowFace/
├── app/                         # 主应用模块
│   └── src/main/java/com/jiaqi/face/
│       ├── FaceDemoActivity.java       # 主页面
│       ├── FaceCameraActivity.java     # 相机页面（实时人脸检测）
│       ├── FaceEngineManager.java      # 人脸核心调用
│       ├── FaceEngineSingleton.java    # 单例管理
│       ├── FaceStore.java              # 内存存储
│       ├── FaceRecord.java             # 数据模型
│       ├── FaceEnrollResult.java       # 录入结果
│       ├── FaceRecognitionResult.java  # 识别结果
│       ├── OverlayView.java            # 人脸框覆盖层
│       └── utils/SimFaceWrapper.kt     # Kotlin 包装类
├── simface/                     # 核心人脸识别 SDK
└── simq/                        # 人脸质量评估库
```

## 核心功能

- 工号输入
- 姓名输入
- 人脸录入（实时检测，质量达标自动抓取）
- 人脸识别
- 1:N 人脸比对
- 清空已录入数据

## 核心参数

| 参数 | 值 |
|------|-----|
| 质量阈值 | 0.4 |
| 匹配阈值 | 0.6 |
| 特征维度 | 512 |

## 编译验证

```powershell
# 设置环境变量
$env:GRADLE_USER_HOME = "D:\app\gradlerepository\.gradle"
$env:JAVA_HOME = "C:\Program Files\BellSoft\LibericaJDK-17"

# 进入项目目录
cd "D:\workspace\jq-apk-ui\Biometrics-SimFace-main\lowFace"

# 仅验证编译（不生成 APK）
.\gradlew.bat compileDebugJavaWithJavac

# 生成 Debug APK
.\gradlew.bat assembleDebug
```

## 构建输出

- APK 路径: `app\build\outputs\apk\debug\app-debug.apk`

## 真机测试

```powershell
# 安装 APK
adb install -r app\build\outputs\apk\debug\app-debug.apk

# 查看日志
adb logcat -s FaceDemo:* FaceEngine:* FaceCamera:*
```

## 测试步骤

1. 启动 App，测试工号和姓名输入是否流畅
2. 输入工号和姓名，点击"录入人脸"
3. 相机页面会实时显示人脸框和质量分数
4. 质量达标（>= 0.4）时自动抓取并录入
5. 点击"识别人脸"进行 1:N 比对

## 注意事项

- 本项目使用原生 XML/View，不使用 Compose
- 所有 SimFace 调用在后台线程执行
- 相机使用 ImageAnalysis 实时检测人脸
- 质量达标时自动抓取，无需手动拍照

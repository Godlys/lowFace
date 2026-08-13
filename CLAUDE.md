# CLAUDE.md

本项目是一个轻量级人脸识别 Demo，使用原生 XML/View 实现，适配低端设备。

**开源项目** - 采用 MIT 许可证发布。

---

## 项目结构

```
lowFace/
├── app/                                 # 主应用模块
│   └── src/
│       ├── main/java/com/low/face/      # 主代码
│       │   ├── FaceDemoActivity.java       # 主页面
│       │   ├── FaceCameraActivity.java     # 相机页面（实时人脸检测）
│       │   ├── FaceEngineManager.java      # 人脸核心调用
│       │   ├── FaceEngineSingleton.java    # 单例管理
│       │   ├── FaceStore.java              # 内存存储
│       │   ├── FaceRecord.java             # 数据模型
│       │   ├── FaceEnrollResult.java       # 录入结果
│       │   ├── FaceRecognitionResult.java  # 识别结果
│       │   ├── OverlayView.java            # 人脸框覆盖层
│       │   └── utils/SimFaceWrapper.kt     # Kotlin 包装类
│       ├── test/java/com/low/face/       # 单元测试
│       │   ├── FaceStoreTest.java
│       │   ├── FaceEnrollResultTest.java
│       │   └── FaceRecognitionResultTest.java
│       └── androidTest/java/com/low/face/ # 仪器测试
│           └── ApplicationTest.java
├── simface/                             # 核心人脸识别 SDK
├── simq/                                # 人脸质量评估库
├── docs/                                # 多语言文档
│   ├── README.md                        # 中文
│   ├── README_EN.md                     # 英文
│   ├── README_VI.md                     # 越南语
│   ├── README_HI.md                     # 印地语
│   ├── README_PT.md                     # 葡萄牙语
│   └── README_ES.md                     # 西班牙语
├── CLAUDE.md                            # 本文档
├── README.md                            # 项目说明
├── CONTRIBUTING.md                      # 贡献指南
├── CHANGELOG.md                         # 更新日志
└── LICENSE                              # MIT 许可证
```

---

## 核心功能

- 工号输入
- 姓名输入
- 人脸录入（实时检测，质量达标自动抓取）
- 人脸识别
- 1:N 人脸比对
- 清空已录入数据

---

## 核心参数

| 参数 | 值 |
|------|-----|
| 质量阈值 | 0.4 |
| 匹配阈值 | 0.6 |
| 特征维度 | 512 |
| 最大记录数 | 100 |

---

## 开源项目标准

### 许可证

本项目采用 **MIT 许可证**，允许：
- 商业使用
- 修改
- 分发
- 私人使用

**要求**：保留版权声明和许可证副本。

### 必需文件

| 文件 | 用途 |
|------|------|
| `LICENSE` | 许可证全文 |
| `README.md` | 项目说明、安装、使用方法 |
| `CONTRIBUTING.md` | 贡献流程、代码规范、签名配置 |
| `CHANGELOG.md` | 版本更新记录 |

### 文档规范

- **多语言支持**: 提供中、英、越、印、葡、西六种语言 README
- **版本号遵循语义化版本**: MAJOR.MINOR.PATCH
- **更新日志遵循 Keep a Changelog 格式**

### 签名配置

开发阶段使用 debug 签名，正式发布需配置独立签名：

```kotlin
// app/build.gradle.kts
signingConfigs {
    create("release") {
        storeFile = file("../lowface.jks")
        storePassword = "your_password"
        keyAlias = "lowface"
        keyPassword = "your_password"
    }
}
```

详见 `CONTRIBUTING.md`。

---

## 测试

### 单元测试

```powershell
# 运行单元测试
.\gradlew.bat test

# 测试报告
# app/build/reports/tests/testDebug/index.html
```

**已覆盖**:
- `FaceStore` - 添加、查询、清空、容量限制
- `FaceEnrollResult` - 成功/失败结果
- `FaceRecognitionResult` - 匹配/未匹配结果

### 仪器测试

```powershell
# 连接设备后运行
.\gradlew.bat connectedAndroidTest
```

**已覆盖**:
- 应用上下文验证

### 测试代码规范

- 单元测试放在 `app/src/test/java/com/low/face/`
- 仪器测试放在 `app/src/androidTest/java/com/low/face/`
- 测试类命名为 `XxxTest.java`
- 使用 JUnit 4 (`@Test`, `@Before`, `@After`)

---

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

# 生成 Release APK
.\gradlew.bat assembleRelease

# 运行测试
.\gradlew.bat test
```

---

## 构建输出

| 类型 | 路径 |
|------|------|
| Debug APK | `app\build\outputs\apk\debug\app-debug.apk` |
| Release APK | `app\build\outputs\apk\release\app-release.apk` |
| 测试报告 | `app\build\reports\tests\testDebug\index.html` |

---

## 真机测试

```powershell
# 安装 APK
adb install -r app\build\outputs\apk\debug\app-debug.apk

# 查看日志
adb logcat -s FaceDemo:* FaceEngine:* FaceCamera:*
```

---

## 测试步骤

1. 启动 App，测试工号和姓名输入是否流畅
2. 输入工号和姓名，点击"录入人脸"
3. 相机页面会实时显示人脸框和质量分数
4. 质量达标（>= 0.4）时自动抓取并录入
5. 点击"识别人脸"进行 1:N 比对

---

## 代码规范

### 禁止使用表情符号

**代码中禁止使用表情符号（emoji）**，包括但不限于：

- 代码注释
- UI 显示文本
- 日志输出
- Release 说明
- 提交信息

**原因**：
1. 不同终端/设备表情显示不一致
2. 部分低端设备无法正确渲染表情
3. 增加代码阅读负担
4. 可能导致编码问题

**错误示例**：
```java
// 错误：注释包含表情
tvStatus.setText("✅ 质量达标");
Log.i(TAG, "★ 首次检测");
```

**正确示例**：
```java
// 正确：使用纯文本
tvStatus.setText("质量达标");
Log.i(TAG, "[首次] 检测");
```

### 其他规范

1. **变量命名**: 使用驼峰命名法，见名知意
2. **注释**: 使用中文注释，清晰简洁
3. **日志**: 使用 `[模块]` 格式标记，便于过滤
4. **代码格式**: 保持缩进一致，避免过长行
5. **颜色资源**: 使用 `res/values/colors.xml`，禁止硬编码
6. **内存管理**: Bitmap 使用后及时 recycle，在 finally 块中执行

---

## 线程安全

### SimFace 调用

所有 SimFace 调用必须在后台线程执行：

| 操作 | 执行线程 |
|------|---------|
| SimFace 初始化 | 后台线程 |
| detectFace | 后台线程 |
| getEmbedding | 后台线程 |
| identificationScore | 后台线程 |
| UI 更新 | 主线程 |

### SimFaceWrapper 说明

`SimFaceWrapper.kt` 使用 `runBlocking` 桥接 Kotlin suspend 函数到 Java 同步调用：

```kotlin
// detectFaces 方法必须从后台线程调用
// FaceEngineManager.executor 确保了这一点
fun detectFaces(bitmap: Bitmap): List<FaceDetection> {
    val result = runBlocking {
        sf.detectFaceBlocking(bitmap)
    }
    return result
}
```

**重要**: 切勿在主线程调用 `SimFaceWrapper.detectFaces()`。

---

## 内存管理

### Bitmap 回收

对齐后的 Bitmap 需要手动回收：

```java
Bitmap alignedFace = null;
try {
    alignedFace = face.alignedFaceImage(sourceBitmap);
    // 处理 alignedFace...
} finally {
    if (alignedFace != null && alignedFace != sourceBitmap && !alignedFace.isRecycled()) {
        alignedFace.recycle();
    }
}
```

### FaceStore 容量限制

防止内存溢出，最大记录数为 100：

```java
private static final int MAX_RECORDS = 100;

public void add(FaceRecord record) {
    while (records.size() >= MAX_RECORDS) {
        records.remove(0); // 移除最旧记录
    }
    records.add(record);
}
```

---

## 性能优化

已实现的优化：

1. **RGBA_8888 直接转换**: 节省约 20ms
2. **ImageProxy 及时释放**: 避免阻塞相机管线
3. **检测节流**: 800ms 间隔，避免过度检测
4. **结果复用**: 避免二次检测
5. **容量限制**: 最大 100 条记录，防止 OOM

---

## 提交规范

提交信息格式：

```
<type>: <subject>

<body>
```

**type 类型**:
- `feat`: 新功能
- `fix`: Bug 修复
- `docs`: 文档更新
- `style`: 代码格式调整
- `refactor`: 重构
- `test`: 测试相关
- `chore`: 构建/工具相关

**示例**:
```
feat: 添加后置摄像头支持

- 支持前后摄像头切换
- 优化相机预览性能
```

---

## 相关链接

- **贡献指南**: `CONTRIBUTING.md`
- **更新日志**: `CHANGELOG.md`
- **许可证**: `LICENSE`
- **多语言文档**: `docs/README_*.md`

---

**语言**: 本项目使用中文进行交流和注释

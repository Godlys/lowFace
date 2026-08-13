# 贡献指南

感谢你对 LowFace 项目的关注！本文档将帮助你了解如何为项目做出贡献。

## 行为准则

- 尊重所有贡献者
- 接受建设性批评
- 关注对社区最有利的事情

## 如何贡献

### 报告 Bug

如果你发现了 Bug，请创建 Issue 并包含以下信息：

1. **标题**: 简洁描述问题
2. **环境**: 设备型号、Android 版本、App 版本
3. **重现步骤**: 详细描述如何重现问题
4. **预期行为**: 你期望发生什么
5. **实际行为**: 实际发生了什么
6. **日志**: 相关的 Logcat 输出（如有）

### 提交功能请求

如果你有新功能建议，请创建 Issue 并说明：

1. 功能描述
2. 使用场景
3. 可能的实现方案（可选）

### 提交代码

#### 开发环境

- JDK 17+
- Android Studio (最新稳定版)
- Android SDK 33+

#### 代码规范

1. **禁止使用表情符号** - 代码、注释、日志中不允许出现 emoji
2. **使用中文注释** - 方便中文开发者阅读
3. **变量命名**: 驼峰命名法
4. **日志格式**: `[模块] 消息内容`

#### 提交规范

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

#### Pull Request 流程

1. Fork 本仓库
2. 创建功能分支 (`git checkout -b feature/your-feature`)
3. 提交更改 (`git commit -m 'feat: 添加某功能'`)
4. 推送到分支 (`git push origin feature/your-feature`)
5. 创建 Pull Request

#### PR 检查清单

- [ ] 代码编译通过
- [ ] 单元测试通过
- [ ] 无表情符号
- [ ] 注释清晰
- [ ] 遵循代码规范

#### 发布签名配置

项目默认使用 debug 签名进行构建。如需发布正式版本，请配置自己的签名：

1. 创建 keystore:
```bash
keytool -genkey -v -keystore lowface.jks -alias lowface -keyalg RSA -keysize 2048 -validity 10000
```

2. 在 `app/build.gradle.kts` 中配置签名:
```kotlin
signingConfigs {
    create("release") {
        storeFile = file("../lowface.jks")
        storePassword = "your_store_password"
        keyAlias = "lowface"
        keyPassword = "your_key_password"
    }
}

buildTypes {
    release {
        signingConfig = signingConfigs.getByName("release")
    }
}
```

3. 或使用 `gradle.properties` 配置（推荐，避免密码泄露）:
```properties
KEYSTORE_FILE=../lowface.jks
KEYSTORE_PASSWORD=your_store_password
KEY_ALIAS=lowface
KEY_PASSWORD=your_key_password
```

**注意**: 切勿将 keystore 文件和密码提交到版本控制。

## 项目结构

```
lowFace/
├── app/                         # 主应用模块
│   └── src/main/java/com/low/face/
├── simface/                     # 核心人脸识别 SDK
└── simq/                        # 人脸质量评估库
```

## 许可证

通过提交代码，你同意你的贡献将按照 MIT 许可证授权。

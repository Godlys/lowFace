plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.low.face"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.low.face"
        minSdk = 24
        targetSdk = 37
        versionCode = 2
        versionName = "2.0"
    }

    // 签名配置
    // 开发时使用 debug keystore，发布时请创建自己的 keystore
    // 参考文档: https://developer.android.com/studio/publish/app-signing
    signingConfigs {
        getByName("debug") {
            // 使用默认 debug keystore
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            // 开发阶段使用 debug 签名，正式发布请配置自己的签名
            // 参考: CONTRIBUTING.md 中的签名配置说明
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources {
            excludes += setOf("META-INF/DEPENDENCIES", "META-INF/LICENSE", "META-INF/LICENSE.txt", "META-INF/NOTICE", "META-INF/NOTICE.txt")
        }
    }
}

dependencies {
    // SimFace 核心模块
    implementation(project(":simface"))

    // AndroidX 核心
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    // CameraX
    implementation(libs.camera.core)
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.view)

    // Lifecycle
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // 单元测试
    testImplementation("junit:junit:4.13.2")

    // 仪器测试
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test:rules:1.5.0")
}

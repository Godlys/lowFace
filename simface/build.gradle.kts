import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.simprints.biometrics.simface"
    compileSdk = 37

    defaultConfig {
        minSdk = 23
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    api(project(":simq"))

    // Tensorflow versions that works with Edgeface
    api(libs.litert)
    api(libs.litert.support)

    // Face Detection and quality
    api(libs.face.detection)

    // For face alignment
    api(libs.ejml.simple)
}

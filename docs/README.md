**English** | [简体中文](../README.md) | [Tiếng Việt](README.vi.md) | [हिन्दी](README.hi.md) | [Português (BR)](README.pt-BR.md) | [Español](README.es.md)

---

# LowFace - Lightweight Face Recognition Demo

> A face recognition application designed specifically for **low-end Android devices**, implemented with native XML/View to verify feasibility on resource-constrained hardware.

## About LowFace
* LowFace: Efficient Face Recognition on Low-End Devices

## Project Origin

This project is developed based on [Simprints Face Biometrics SDK](https://github.com/Simprints/Biometrics-SimFace), retaining the core face recognition capabilities while completely rewriting the UI layer:

- **Original Project**: Built with Jetpack Compose for modern UI
- **This Project**: Built with native XML/View, optimized for low-end devices

## Features

- Employee ID/Name input
- Face enrollment (automatic capture when quality threshold is met)
- Face recognition (1:N matching)
- Real-time face bounding box display
- Quality score indication

## Core Parameters

| Parameter | Value | Description |
|-----------|-------|-------------|
| Quality Threshold | 0.4 | Face quality judgment threshold |
| Match Threshold | 0.85 | 1:N matching threshold |
| Feature Dimension | 512 | EdgeFace output embedding dimension |

## Tech Stack

- **UI**: Native XML/View (no Compose)
- **Camera**: CameraX + PreviewView
- **Face Detection**: Google ML Kit (via SimFace SDK)
- **Feature Extraction**: EdgeFace TFLite model
- **Languages**: Java + Kotlin (SDK layer only)

## Project Structure

```
lowFace/
├── app/                         # Main application module
│   └── src/main/java/com/low/face/
│       ├── FaceDemoActivity.java       # Main activity
│       ├── FaceCameraActivity.java     # Camera activity
│       ├── FaceEngineManager.java      # Face core operations
│       ├── FaceEngineSingleton.java    # Singleton manager
│       ├── FaceStore.java              # In-memory storage
│       ├── FaceRecord.java             # Data model
│       ├── OverlayView.java            # Face overlay view
│       └── utils/SimFaceWrapper.kt     # Kotlin wrapper
├── simface/                     # Core face recognition SDK
└── simq/                        # Face quality assessment library
```

## Build & Run

### Requirements

- JDK 17+
- Android SDK 33+
- Gradle 9.6.1+

### Build Commands

```powershell

# Enter project directory
cd lowFace

# Compile verification
.\gradlew.bat compileDebugJavaWithJavac

# Build Debug APK
.\gradlew.bat assembleDebug
```

### Install & Test

```powershell
# Install to device
adb install -r app\build\outputs\apk\debug\app-debug.apk

# View logs
adb logcat -s FaceDemo:* FaceEngine:* FaceCamera:*
```

---

## Low-End Device Adaptation (Key Focus)

### Target Device Specifications

This project is optimized for low-end devices with the following specifications:

| Item | Specification |
|------|---------------|
| CPU | MediaTek MT6762 (4-core 2.0GHz) |
| RAM | 2GB |
| Storage | 32GB |
| Android Version | 10-11 |

### Why Not Compose?

Jetpack Compose has the following issues on low-end devices:

1. **Slow Initial Load**: Compose runtime initialization + first recomposition takes 200-500ms
2. **High Memory Usage**: Compose base library is about 2-3MB, a burden for 2GB RAM devices
3. **Input Latency**: Complex recomposition can cause input field lag
4. **Long Cold Start**: Time from icon tap to interactive state is longer

This project chooses **native XML/View**:

- Zero extra dependency overhead
- System-level rendering optimization
- More direct input response
- Lower memory footprint

### Performance Optimizations

#### 1. Image Processing Optimization

| Optimization | Solution | Effect |
|--------------|----------|--------|
| Bitmap Conversion | Direct RGBA_8888 conversion, skip YUV→JPEG→decode | Save ~20ms |
| ImageProxy Release | Close immediately after Bitmap conversion, before detection | Avoid blocking camera pipeline |
| Image Scaling | Analysis image limited to 480×640 | resizeBitmap takes 0ms |

#### 2. Detection Throttling

- Detection interval: **800ms**
- Use `AtomicBoolean` to prevent concurrent detection
- Non-detection frames are closed immediately, not consuming CPU

#### 3. Result Reuse

Reuse detection results from preview frames during auto-capture to avoid redundant detection:

```
Before: Preview detection → Auto capture → Re-detection(400ms) → Feature extraction
After:  Preview detection → Auto capture → Direct feature extraction
```

Saves **400-500ms**.

### Real Device Performance Data

#### Cold Start (First Run)

| Stage | Duration |
|-------|----------|
| setContentView | 217-248ms |
| Camera initialization | 267-278ms |
| bindToLifecycle | 278ms |
| First frame arrival | 1200-1400ms from onCreate |
| First face detection | 1000-1100ms |
| First feature extraction | 900-950ms |

#### Stable Operation (After Warm-up)

| Stage | Duration |
|-------|----------|
| Face detection | 400-530ms |
| Face alignment | 100-130ms |
| Feature extraction | 90-100ms |
| 1:N matching (10 people) | 10-15ms |
| Post auto-capture processing | ~230ms |

---

## Current Limitations

### 1. Detection Speed Limited

- **Cause**: ML Kit face detection takes 400-500ms/frame on low-end CPU
- **Impact**: Cannot achieve smooth real-time frame-by-frame detection
- **Current State**: Using 800ms throttling + auto-capture solution

### 2. Slow Cold Start

- **Cause**: Model loading, OpenCV initialization, CPU frequency scaling
- **Impact**: Slow response for first enrollment/recognition
- **Current State**: No perfect solution yet, recommend warm-up

### 3. In-Memory Storage

- **Current State**: Enrolled data only stored in memory
- **Impact**: Need to re-enroll after app restart
- **Plan**: Future version will support persistent storage

### 4. No Liveness Detection

- **Current State**: Recognition based on photos only
- **Risk**: May be spoofed by photos
- **Plan**: Need to integrate liveness detection solution

### 5. Single Camera Support

- **Current State**: Front camera only
- **Impact**: May be inconvenient in some scenarios
- **Plan**: Future support for camera switching

### 6. Input Experience Not Fully Verified

- **Current State**: Whether input fields remain smooth after SDK initialization not fully verified
- **Risk**: Possible input latency on low-end devices
- **Suggestion**: Need further testing for "focus gained → first character input" timing

---

## Comparison with Original Compose Version

| Item | Original (Compose) | This Project (XML/View) |
|------|-------------------|-------------------------|
| UI Framework | Jetpack Compose | Native XML/View |
| First Screen Load | Slower | Faster |
| Memory Usage | Higher | Lower |
| Input Response | May lag | More fluid |
| Development Efficiency | High | Medium |
| Maintenance Cost | Low | Medium |

---

## Future Optimization Directions

1. **Persistent Storage**: Use SQLite or SharedPreferences to save enrolled faces
2. **Liveness Detection**: Integrate blink/mouth movement detection
3. **Camera Warm-up**: Pre-warm camera and model in main activity background
4. **Rear Camera**: Support front/rear camera switching
5. **Batch Enrollment**: Support enrolling multiple people at once
6. **NPU Acceleration**: Utilize NPU for inference acceleration if device supports

---

## License

The core SDK (`simface`, `simq`) follows the original project license.

Application layer code is licensed under MIT License, free to use and modify.

---

## Acknowledgments

- [Simprints](https://simprints.com/) - For open source face recognition SDK
- [Google ML Kit](https://developers.google.com/ml-kit) - Face detection capability
- [EdgeFace](https://github.com/SeetaFace6Open/SeetaFace6Open) - Feature extraction model

---

## Project Value & Significance

In today's rapidly developing face recognition technology, many solutions default to running on mid-to-high-end smart devices or cloud servers. However, there are still many resource-constrained usage scenarios: cost-sensitive devices, limited network conditions, insufficient computing resources, yet still requiring basic identity authentication capabilities.

LowFace's goal is not to pursue the highest recognition accuracy in laboratory environments, but to explore **achieving usable face recognition capabilities on low-end Android devices**, enabling more existing devices to have digital authentication capabilities.

For many developing countries, remote areas, and cost-sensitive enterprises, many identity verification scenarios do not require financial-grade, security-level face recognition systems, but rather need a lightweight solution that is:

- Low cost
- Can run offline
- Low network dependency
- Can be deployed on existing devices

Examples include:

- Internal enterprise attendance and employee sign-in
- Small organization personnel management
- Educational training scenario identity confirmation
- Basic access control and device authorization
- Community or grassroots service identity verification

These scenarios focus more on "reliability and ease of deployment" rather than pursuing maximum recognition metrics in extreme environments.

At the same time, LowFace also focuses on extending the lifecycle of electronic devices. Many older Android devices cannot run modern applications due to insufficient performance, but their cameras, screens, and basic computing capabilities can still meet many lightweight task requirements. Through optimization for low-end hardware, these devices can continue to create value and reduce electronic waste generation.

From an environmental perspective, bringing old devices back into production and service scenarios is essentially a form of resource reuse:

- Reduce new hardware procurement needs
- Extend device usage cycle
- Reduce electronic waste
- Lower digital infrastructure construction costs

LowFace hopes to explore a more inclusive technical approach:

> Not upgrading all devices to high-performance hardware, but enabling more existing devices to continue creating value through software optimization.
> Advanced capabilities should not only belong to high-performance devices, but should serve more real scenarios at lower cost and more broadly.

This is the significance of low-end device optimization, lightweight face recognition, and edge AI technology in the real world.

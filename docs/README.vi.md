[English](README.md) | [简体中文](../README.md) | **Tiếng Việt** | [हिन्दी](README.hi.md) | [Português (BR)](README.pt-BR.md) | [Español](README.es.md)

---

# LowFace - Ứng dụng Nhận diện Khuôn mặt Nhẹ

> Ứng dụng nhận diện khuôn mặt được thiết kế đặc biệt cho **thiết bị Android cấp thấp**, sử dụng XML/View gốc để xác minh tính khả thi trên phần cứng hạn chế tài nguyên.

## Về LowFace
* LowFace: Nhận diện Khuôn mặt Hiệu quả trên Thiết bị Cấp thấp

## Nguồn gốc Dự án

Dự án này được phát triển dựa trên [Simprints Face Biometrics SDK](https://github.com/Simprints/Biometrics-SimFace), giữ lại các khả năng nhận diện khuôn mặt cốt lõi trong khi viết lại hoàn toàn lớp UI:

- **Dự án Gốc**: Được xây dựng bằng Jetpack Compose cho UI hiện đại
- **Dự án Này**: Được xây dựng bằng XML/View gốc, tối ưu cho thiết bị cấp thấp

## Tính năng

- Nhập mã nhân viên/tên
- Đăng ký khuôn mặt (tự động chụp khi đạt ngưỡng chất lượng)
- Nhận diện khuôn mặt (khớp 1:N)
- Hiển thị khung khuôn mặt theo thời gian thực
- Hiển thị điểm chất lượng

## Tham số Cốt lõi

| Tham số | Giá trị | Mô tả |
|---------|---------|-------|
| Ngưỡng Chất lượng | 0.4 | Ngưỡng đánh giá chất lượng khuôn mặt |
| Ngưỡng Khớp | 0.85 | Ngưỡng khớp 1:N |
| Kích thước Đặc trưng | 512 | Kích thước vector đặc trưng EdgeFace |

## Công nghệ

- **UI**: XML/View gốc (không dùng Compose)
- **Camera**: CameraX + PreviewView
- **Phát hiện Khuôn mặt**: Google ML Kit (qua SimFace SDK)
- **Trích xuất Đặc trưng**: Mô hình EdgeFace TFLite
- **Ngôn ngữ**: Java + Kotlin (chỉ lớp SDK)

## Cấu trúc Dự án

```
lowFace/
├── app/                         # Module ứng dụng chính
│   └── src/main/java/com/low/face/
│       ├── FaceDemoActivity.java       # Activity chính
│       ├── FaceCameraActivity.java     # Activity camera
│       ├── FaceEngineManager.java      # Thao tác khuôn mặt cốt lõi
│       ├── FaceEngineSingleton.java    # Quản lý singleton
│       ├── FaceStore.java              # Lưu trữ trong bộ nhớ
│       ├── FaceRecord.java             # Mô hình dữ liệu
│       ├── OverlayView.java            # View overlay khuôn mặt
│       └── utils/SimFaceWrapper.kt     # Wrapper Kotlin
├── simface/                     # SDK nhận diện khuôn mặt cốt lõi
└── simq/                        # Thư viện đánh giá chất lượng khuôn mặt
```

## Xây dựng & Chạy

### Yêu cầu

- JDK 17+
- Android SDK 33+
- Gradle 9.6.1+

### Lệnh Xây dựng

```powershell

# Vào thư mục dự án
cd lowFace

# Xác minh biên dịch
.\gradlew.bat compileDebugJavaWithJavac

# Xây dựng Debug APK
.\gradlew.bat assembleDebug
```

### Cài đặt & Kiểm tra

```powershell
# Cài đặt vào thiết bị
adb install -r app\build\outputs\apk\debug\app-debug.apk

# Xem nhật ký
adb logcat -s FaceDemo:* FaceEngine:* FaceCamera:*
```

---

## Thích ứng Thiết bị Cấp thấp (Trọng điểm)

### Thông số Thiết bị Mục tiêu

Dự án này được tối ưu cho thiết bị cấp thấp với thông số sau:

| Mục | Thông số |
|-----|----------|
| CPU | MediaTek MT6762 (4 nhân 2.0GHz) |
| RAM | 2GB |
| Bộ nhớ | 32GB |
| Phiên bản Android | 10-11 |

### Tại sao không dùng Compose?

Jetpack Compose có các vấn đề sau trên thiết bị cấp thấp:

1. **Tải ban đầu chậm**: Khởi tạo runtime Compose + recomposition đầu tiên mất 200-500ms
2. **Sử dụng bộ nhớ cao**: Thư viện cơ sở Compose khoảng 2-3MB, gánh nặng cho thiết bị RAM 2GB
3. **Độ trễ đầu vào**: Recomposition phức tạp có thể gây lag trường nhập
4. **Khởi động nguôi dài**: Thời gian từ khi nhấn biểu tượng đến trạng thái tương tác lâu hơn

Dự án này chọn **XML/View gốc**:

- Không có chi phí phụ thuộc bổ sung
- Tối ưu hiển thị cấp hệ thống
- Phản hồi đầu vào trực tiếp hơn
- Dấu chân bộ nhớ thấp hơn

### Tối ưu Hiệu suất

#### 1. Tối ưu Xử lý Hình ảnh

| Tối ưu | Giải pháp | Hiệu quả |
|--------|-----------|----------|
| Chuyển đổi Bitmap | Chuyển đổi RGBA_8888 trực tiếp, bỏ YUV→JPEG→decode | Tiết kiệm ~20ms |
| Giải phóng ImageProxy | Đóng ngay sau chuyển đổi Bitmap, trước phát hiện | Tránh chặn pipeline camera |
| Co giãn hình ảnh | Hình ảnh phân tích giới hạn 480×640 | resizeBitmap mất 0ms |

#### 2. Điều tiết Phát hiện

- Khoảng cách phát hiện: **800ms**
- Sử dụng `AtomicBoolean` để ngăn phát hiện đồng thời
- Các khung không phát hiện được đóng ngay, không tiêu tốn CPU

#### 3. Tái sử dụng Kết quả

Tái sử dụng kết quả phát hiện từ khung xem trước trong khi tự động chụp để tránh phát hiện lặp lại:

```
Trước: Phát hiện xem trước → Tự động chụp → Phát hiện lại(400ms) → Trích xuất đặc trưng
Sau:  Phát hiện xem trước → Tự động chụp → Trích xuất đặc trưng trực tiếp
```

Tiết kiệm **400-500ms**.

### Dữ liệu Hiệu suất Thiết bị Thực

#### Khởi động Nguôi (Chạy đầu tiên)

| Giai đoạn | Thời lượng |
|-----------|------------|
| setContentView | 217-248ms |
| Khởi tạo camera | 267-278ms |
| bindToLifecycle | 278ms |
| Khung đầu tiên đến | 1200-1400ms từ onCreate |
| Phát hiện khuôn mặt đầu tiên | 1000-1100ms |
| Trích xuất đặc trưng đầu tiên | 900-950ms |

#### Hoạt động Ổn định (Sau khi làm nóng)

| Giai đoạn | Thời lượng |
|-----------|------------|
| Phát hiện khuôn mặt | 400-530ms |
| Căn chỉnh khuôn mặt | 100-130ms |
| Trích xuất đặc trưng | 90-100ms |
| Khớp 1:N (10 người) | 10-15ms |
| Xử lý sau tự động chụp | ~230ms |

---

## Hạn chế Hiện tại

### 1. Tốc độ Phát hiện Giới hạn

- **Nguyên nhân**: Phát hiện khuôn mặt ML Kit mất 400-500ms/khung trên CPU cấp thấp
- **Tác động**: Không thể đạt được phát hiện theo từng khung theo thời gian thực mượt mà
- **Trạng thái Hiện tại**: Sử dụng giải pháp điều tiết 800ms + tự động chụp

### 2. Khởi động Nguôi Chậm

- **Nguyên nhân**: Tải mô hình, khởi tạo OpenCV, chia tần số CPU
- **Tác động**: Phản hồi chậm cho đăng ký/nhận diện đầu tiên
- **Trạng thái Hiện tại**: Chưa có giải pháp hoàn hảo, khuyến nghị làm nóng trước

### 3. Lưu trữ trong Bộ nhớ

- **Trạng thái Hiện tại**: Dữ liệu đã đăng ký chỉ lưu trong bộ nhớ
- **Tác động**: Cần đăng ký lại sau khi khởi động lại ứng dụng
- **Kế hoạch**: Phiên bản tương lai sẽ hỗ trợ lưu trữ bền vững

### 4. Không có Phát hiện Sống

- **Trạng thái Hiện tại**: Nhận diện dựa trên ảnh only
- **Rủi ro**: Có thể bị lừa bằng ảnh
- **Kế hoạch**: Cần tích hợp giải pháp phát hiện sống

### 5. Hỗ trợ Một Camera

- **Trạng thái Hiện tại**: Chỉ camera trước
- **Tác động**: Có thể bất tiện trong một số tình huống
- **Kế hoạch**: Hỗ trợ chuyển đổi camera trong tương lai

### 6. Trải nghiệm Đầu vào chưa Xác minh Đầy đủ

- **Trạng thái Hiện tại**: Trường nhập có còn mượt sau khi khởi tạo SDK chưa được xác minh đầy đủ
- **Rủi ro**: Có thể có độ trễ đầu vào trên thiết bị cấp thấp
- **Gợi ý**: Cần kiểm tra thêm về thời gian "nhận focus → nhập ký tự đầu tiên"

---

## So sánh với Phiên bản Compose Gốc

| Mục | Gốc (Compose) | Dự án Này (XML/View) |
|-----|---------------|----------------------|
| Framework UI | Jetpack Compose | XML/View Gốc |
| Tải màn hình đầu | Chậm hơn | Nhanh hơn |
| Sử dụng Bộ nhớ | Cao hơn | Thấp hơn |
| Phản hồi Đầu vào | Có thể lag | Mượt hơn |
| Hiệu quả Phát triển | Cao | Trung bình |
| Chi phí Bảo trì | Thấp | Trung bình |

---

## Hướng Tối ưu Hóa Tương lai

1. **Lưu trữ Bền vững**: Sử dụng SQLite hoặc SharedPreferences để lưu khuôn mặt đã đăng ký
2. **Phát hiện Sống**: Tích hợp phát hiện chớp mắt/mở miệng
3. **Làm nóng Camera**: Làm nóng trước camera và mô hình trong nền activity chính
4. **Camera Sau**: Hỗ trợ chuyển đổi camera trước/sau
5. **Đăng ký Hàng loạt**: Hỗ trợ đăng ký nhiều người cùng lúc
6. **Tăng tốc NPU**: Tận dụng NPU cho tăng tốc suy luận nếu thiết bị hỗ trợ

---

## Giấy phép

SDK cốt lõi (`simface`, `simq`) tuân theo giấy phép dự án gốc.

Mã lớp ứng dụng được cấp phép theo MIT License, tự do sử dụng và chỉnh sửa.

---

## Lời cảm ơn

- [Simprints](https://simprints.com/) - Cho SDK nhận diện khuôn mặt mã nguồn mở
- [Google ML Kit](https://developers.google.com/ml-kit) - Khả năng phát hiện khuôn mặt
- [EdgeFace](https://github.com/SeetaFace6Open/SeetaFace6Open) - Mô hình trích xuất đặc trưng

---

## Giá trị & Ý nghĩa Dự án

Trong bối cảnh công nghệ nhận diện khuôn mặt phát triển nhanh chóng hiện nay, nhiều giải pháp mặc định chạy trên thiết bị thông minh cấp trung-cao hoặc máy chủ đám mây. Tuy nhiên, vẫn còn nhiều kịch bản sử dụng hạn chế tài nguyên: thiết bị nhạy cảm chi phí, điều kiện mạng hạn chế, tài nguyên tính toán không đủ, nhưng vẫn cần khả năng xác thực danh tính cơ bản.

Mục tiêu của LowFace không phải là theo đuổi độ chính xác nhận diện cao nhất trong môi trường phòng thí nghiệm, mà là khám phá **đạt được khả năng nhận diện khuôn mặt sử dụng được trên thiết bị Android cấp thấp**, cho phép nhiều thiết bị hiện có có khả năng xác thực số hóa.

Đối với nhiều quốc gia đang phát triển, vùng sâu vùng xa và doanh nghiệp nhạy cảm về chi phí, nhiều kịch bản xác minh danh tính không yêu cầu hệ thống nhận diện khuôn mặt cấp độ tài chính, an ninh, mà cần một giải pháp nhẹ:

- Chi phí thấp
- Có thể chạy ngoại tuyến
- Phụ thuộc mạng thấp
- Có thể triển khai trên thiết bị hiện có

Ví dụ bao gồm:

- Chấm công nội bộ doanh nghiệp và đăng ký nhân viên
- Quản lý nhân viên tổ chức nhỏ
- Xác nhận danh tính trong kịch bản đào tạo giáo dục
- Kiểm soát truy cập cơ bản và ủy quyền thiết bị
- Xác minh danh tính dịch vụ cộng đồng hoặc cơ sở

Các kịch bản này tập trung nhiều hơn vào "độ tin cậy và dễ triển khai" thay vì theo đuổi chỉ số nhận diện tối đa trong môi trường cực đoan.

Đồng thời, LowFace cũng tập trung vào việc kéo dài vòng đời của thiết bị điện tử. Nhiều thiết bị Android cũ không thể chạy ứng dụng hiện đại do hiệu suất không đủ, nhưng camera, màn hình và khả năng tính toán cơ bản của chúng vẫn có thể đáp ứng nhiều yêu cầu tác vụ nhẹ. Thông qua tối ưu hóa cho phần cứng cấp thấp, các thiết bị này có thể tiếp tục tạo ra giá trị và giảm thiểu lãng phí điện tử.

Từ góc độ môi trường, đưa các thiết bị cũ trở lại các kịch bản sản xuất và dịch vụ về cơ bản là một hình thức tái sử dụng tài nguyên:

- Giảm nhu cầu mua sắm phần cứng mới
- Kéo dài vòng đời sử dụng thiết bị
- Giảm rác thải điện tử
- Giảm chi phí xây dựng cơ sở hạ tầng kỹ thuật số

LowFace hy vọng khám phá một cách tiếp cận kỹ thuật bao trùm hơn:

> Không phải nâng cấp tất cả thiết bị lên phần cứng hiệu suất cao, mà cho phép nhiều thiết bị hiện có tiếp tục tạo ra giá trị thông qua tối ưu hóa phần mềm.
> Khả năng tiên tiến không chỉ nên thuộc về thiết bị hiệu suất cao, mà nên phục vụ nhiều kịch bản thực tế hơn với chi phí thấp hơn và rộng rãi hơn.

Đây là ý nghĩa của tối ưu hóa thiết bị cấp thấp, nhận diện khuôn mặt nhẹ và công nghệ AI cạnh trong thế giới thực.

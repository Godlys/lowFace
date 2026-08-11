[English](README.md) | [简体中文](../README.md) | **Tiếng Việt** | [हिन्दी](README.hi.md) | [Português (BR)](README.pt-BR.md) | [Español](README.es.md)

---

# LowFace - Ứng dụng Nhận diện Khuôn mặt Nhẹ

> Ứng dụng nhận diện khuôn mặt được thiết kế đặc biệt cho **thiết bị Android cấp thấp**, sử dụng XML/View gốc để xác minh tính khả thi trên phần cứng hạn chế tài nguyên.

## Về LowFace
* LowFace: Nhận diện Khuôn mặt Hiệu quả trên Thiết bị Cấp thấp

---

**📝 Bản dịch này đang được hoàn thiện.**

Nếu bạn thông thạo tiếng Việt, chúng tôi rất hoan nghênh đóng góp của bạn để hoàn thiện tài liệu này. Vui lòng tham khảo [phiên bản tiếng Anh](README.md) hoặc [phiên bản tiếng Trung](../README.md) để biết nội dung đầy đủ.

---

## Tính năng chính

- Nhập mã nhân viên/tên
- Đăng ký khuôn mặt (tự động chụp khi đạt ngưỡng chất lượng)
- Nhận diện khuôn mặt (khớp 1:N)
- Hiển thị khung khuôn mặt theo thời gian thực
- Hiển thị điểm chất lượng

## Tham số cốt lõi

| Tham số | Giá trị | Mô tả |
|---------|---------|-------|
| Ngưỡng chất lượng | 0.4 | Ngưỡng đánh giá chất lượng khuôn mặt |
| Ngưỡng khớp | 0.85 | Ngưỡng khớp 1:N |
| Kích thước đặc trưng | 512 | Kích thước vector đặc trưng EdgeFace |

## Công nghệ

- **UI**: XML/View gốc (không sử dụng Compose)
- **Camera**: CameraX + PreviewView
- **Phát hiện khuôn mặt**: Google ML Kit
- **Trích xuất đặc trưng**: EdgeFace TFLite model

## Xây dựng

```powershell
cd lowFace
.\gradlew.bat assembleDebug
```

---

## Giá trị Dự án

LowFace nhằm mục đích khám phá khả năng **thực hiện nhận diện khuôn mặt khả dụng trên thiết bị Android cấp thấp**, cho phép nhiều thiết bị hiện có có khả năng xác thực số hóa.

Đối với nhiều quốc gia đang phát triển, vùng sâu vùng xa và doanh nghiệp nhạy cảm về chi phí, nhiều kịch bản xác minh danh tính không yêu cầu hệ thống nhận diện khuôn mặt cấp độ tài chính, an ninh, mà cần một giải pháp nhẹ:

- Chi phí thấp
- Có thể chạy ngoại tuyến
- Phụ thuộc mạng thấp
- Có thể triển khai trên thiết bị hiện có

---

## Đóng góp

Chúng tôi hoan nghênh các đóng góp dịch thuật! Vui lòng tạo Pull Request để cải thiện bản dịch này.

## Giấy phép

SDK cốt lõi (`simface`, `simq`) tuân theo giấy phép dự án gốc.

Mã ứng dụng được cấp phép theo MIT License.

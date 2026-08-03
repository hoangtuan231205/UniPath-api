# Tài liệu API Quản lý CV AI (CV Profile API)

Tài liệu chi tiết các API tạo, cập nhật, xoá và thiết lập CV mặc định cho Ứng viên cho dự án **UniPath API**.

---

## 📌 Thông tin chung
- **Base URL:** `/api/cv-profiles`
- **Xác thực:** Yêu cầu Header `Authorization: Bearer <token>`.

---

## 📑 Danh sách API

### 1. Lấy danh sách CV của tôi (`GET /api/cv-profiles`)
- **HTTP Method:** `GET`
- **URL Path:** `/api/cv-profiles`
- **Mô tả:** Trả về danh sách tất cả các bản CV được AI/người dùng tạo ra (`List<CvProfileResponse>`).

---

### 2. Tạo bản CV mới (`POST /api/cv-profiles`)
- **HTTP Method:** `POST`
- **URL Path:** `/api/cv-profiles`
- **Mô tả:** Tạo mới một mẫu hồ sơ CV.

#### Request Body (CvProfileRequest)
| Trường | Kiểu dữ liệu | Bắt buộc | Mô tả |
| :--- | :--- | :---: | :--- |
| `templateId` | `Integer` | Không | ID mẫu giao diện CV (`cv_templates`) |
| `title` | `String` | Có | Tiêu đề đặt cho bản CV |
| `contentJson` | `String` | Có | Dữ liệu cấu trúc CV dạng JSON String (`{summary, experience, education, skills,...}`) |

#### Ví dụ Request
```json
{
  "templateId": 1,
  "title": "CV Lập trình viên Java Web 2026",
  "contentJson": "{\"summary\":\"Lập trình viên backend 2 năm kinh nghiệm\",\"skills\":[\"Java\",\"Spring Boot\",\"PostgreSQL\"]}"
}
```

---

### 3. Cập nhật nội dung CV (`PUT /api/cv-profiles/{id}`)
- **HTTP Method:** `PUT`
- **URL Path:** `/api/cv-profiles/{id}`

---

### 4. Xoá bản CV (`DELETE /api/cv-profiles/{id}`)
- **HTTP Method:** `DELETE`
- **URL Path:** `/api/cv-profiles/{id}`

---

### 5. Đặt làm CV chính (`PATCH /api/cv-profiles/{id}/set-primary`)
- **HTTP Method:** `PATCH`
- **URL Path:** `/api/cv-profiles/{id}/set-primary`
- **Mô tả:** Đặt cờ `isPrimary = true` cho bản CV này, đồng thời tự động reset cờ `isPrimary = false` đối với toàn bộ các bản CV khác của người dùng.

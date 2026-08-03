# Tài liệu API Lưu tin Tuyển dụng (Saved Job API)

Tài liệu chi tiết các API lưu và quản lý danh sách tin tuyển dụng yêu thích của Ứng viên cho dự án **UniPath API**.

---

## 📌 Thông tin chung
- **Base URL:** `/api/saved-jobs` & `/api/jobs/{id}/save`
- **Xác thực:** Yêu cầu Header `Authorization: Bearer <token>` của tài khoản Ứng viên.

---

## 📑 Danh sách API

### 1. Lưu tin tuyển dụng (`POST /api/jobs/{id}/save`)
- **HTTP Method:** `POST`
- **URL Path:** `/api/jobs/{id}/save`
- **Mô tả:** Đánh dấu lưu tin tuyển dụng vào danh sách yêu thích của ứng viên.

---

### 2. Bỏ lưu tin tuyển dụng (`DELETE /api/jobs/{id}/save`)
- **HTTP Method:** `DELETE`
- **URL Path:** `/api/jobs/{id}/save`
- **Mô tả:** Bỏ lưu tin tuyển dụng khỏi danh sách yêu thích.

---

### 3. Xem danh sách tin tuyển dụng đã lưu (`GET /api/saved-jobs`)
- **HTTP Method:** `GET`
- **URL Path:** `/api/saved-jobs`
- **Mô tả:** Trả về danh sách các tin tuyển dụng mà Ứng viên đã lưu, sắp xếp theo thời gian lưu mới nhất (`List<JobResponse>`).

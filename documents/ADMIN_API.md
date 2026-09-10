# Tài liệu API Quản trị Hệ thống (Admin API)

Tài liệu chi tiết tất cả các API dành cho Quản trị viên (System Admin) bao gồm phê duyệt doanh nghiệp, quản lý tài khoản người dùng, xử lý báo cáo vi phạm, quản lý danh mục/kỹ năng và báo cáo thống kê trong **UniPath API**.

---

## 📌 Thông tin chung
- **Base URL:** `/api/admin` (Và `/api/jobs/{id}/report`)
- **CORS:** Cho phép tất cả origins (`@CrossOrigin(origins = "*")`)
- **Xác thực:** Yêu cầu Header `Authorization: Bearer <token>` có quyền `ADMIN`.
- **Định dạng dữ liệu:** `application/json`

---

## 🏢 1. Phê duyệt Doanh nghiệp (AdminCompanyController)

### 1.1 Danh sách Công ty chờ duyệt (`GET /api/admin/companies`)
- **HTTP Method:** `GET`
- **URL Path:** `/api/admin/companies`
- **Query Parameters:** `status` (Mặc định: `PENDING`, các giá trị khác: `APPROVED`, `REJECTED`)
- **Mô tả:** Lấy danh sách các công ty do Nhà tuyển dụng gửi đề xuất đang chờ Admin duyệt.

---

### 1.2 Phê duyệt Công ty (`PATCH /api/admin/companies/{id}/approve`)
- **HTTP Method:** `PATCH`
- **URL Path:** `/api/admin/companies/{id}/approve`
- **Mô tả:** Chấp nhận đề xuất doanh nghiệp. Đổi trạng thái `status = 'APPROVED'`, ghi nhận người duyệt và thời gian duyệt.

---

### 1.3 Từ chối Công ty (`PATCH /api/admin/companies/{id}/reject`)
- **HTTP Method:** `PATCH`
- **URL Path:** `/api/admin/companies/{id}/reject`
- **Mô tả:** Từ chối đề xuất doanh nghiệp. Đổi trạng thái `status = 'REJECTED'`.

---

## 👥 2. Quản lý Tài khoản Người dùng (AdminController)

### 2.1 Danh sách Tài khoản người dùng (`GET /api/admin/users`)
- **HTTP Method:** `GET`
- **URL Path:** `/api/admin/users`
- **Query Parameters:**
  - `type` (`String`, tùy chọn): Vai trò (`CANDIDATE`, `EMPLOYER`, `ADMIN`)
  - `status` (`Boolean`, tùy chọn): Trạng thái hoạt động (`true`: Đang hoạt động, `false`: Bị khóa)
  - `search` (`String`, tùy chọn): Từ khóa tìm kiếm theo Email hoặc Họ tên

---

### 2.2 Khóa tài khoản người dùng (`PATCH /api/admin/users/{id}/ban`)
- **HTTP Method:** `PATCH`
- **URL Path:** `/api/admin/users/{id}/ban`
- **Mô tả:** Khóa tài khoản người dùng vi phạm. Đổi trạng thái tài khoản thành bị cấm hoạt động (`isBanned = true`).

---

### 2.3 Mở khóa tài khoản người dùng (`PATCH /api/admin/users/{id}/unban`)
- **HTTP Method:** `PATCH`
- **URL Path:** `/api/admin/users/{id}/unban`
- **Mô tả:** Mở khóa lại tài khoản người dùng đã bị cấm trước đó.

---

## 🚩 3. Hệ thống Báo cáo Vi phạm (Reporting System)

### 3.1 Gửi báo cáo vi phạm bài đăng tin tuyển dụng (`POST /api/jobs/{id}/report`)
- **HTTP Method:** `POST`
- **URL Path:** `/api/jobs/{id}/report`
- **Mô tả:** Người dùng (Ứng viên) báo cáo tin tuyển dụng vi phạm quy chuẩn.

#### Request Body (ReportRequest)
```json
{
  "reason": "Tin tuyển dụng có dấu hiệu lừa đảo, yêu cầu nộp phí đặt cọc"
}
```

---

### 3.2 Xem danh sách Báo cáo vi phạm (`GET /api/admin/reports`)
- **HTTP Method:** `GET`
- **URL Path:** `/api/admin/reports`
- **Query Parameters:** `status` (`PENDING`, `RESOLVED`, `REJECTED`)
- **Mô tả:** Admin xem danh sách các báo cáo vi phạm do người dùng gửi lên.

---

### 3.3 Xử lý Báo cáo vi phạm (`PATCH /api/admin/reports/{id}/resolve`)
- **HTTP Method:** `PATCH`
- **URL Path:** `/api/admin/reports/{id}/resolve`
- **Mô tả:** Admin ra quyết định xử lý báo cáo vi phạm.

#### Request Body (ReportResolveRequest)
```json
{
  "action": "DELETE_POST"
}
```
*(Các hành động `action`: `REJECT` - Bác bỏ báo cáo, `DELETE_POST` - Xóa tin tuyển dụng, `BAN_ACCOUNT` - Khóa tài khoản đăng bài).*

---

## 🏷️ 4. Quản lý Danh mục Công việc (Categories)

### 4.1 Lấy danh sách danh mục (`GET /api/admin/categories`)
- **HTTP Method:** `GET`
- **URL Path:** `/api/admin/categories`

---

### 4.2 Tạo mới danh mục (`POST /api/admin/categories`)
- **HTTP Method:** `POST`
- **URL Path:** `/api/admin/categories`
- **Request Body (CategoryRequest):**
  ```json
  {
    "name": "Công nghệ thông tin"
  }
  ```

---

### 4.3 Cập nhật danh mục (`PUT /api/admin/categories/{id}`)
- **HTTP Method:** `PUT`
- **URL Path:** `/api/admin/categories/{id}`
- **Request Body (CategoryRequest):**
  ```json
  {
    "name": "Công nghệ thông tin & Phần mềm"
  }
  ```

---

### 4.4 Xóa danh mục (`DELETE /api/admin/categories/{id}`)
- **HTTP Method:** `DELETE`
- **URL Path:** `/api/admin/categories/{id}`

---

## 🛠️ 5. Quản lý Kỹ năng (Skills)

### 5.1 Lấy danh sách kỹ năng (`GET /api/admin/skills`)
- **HTTP Method:** `GET`
- **URL Path:** `/api/admin/skills`

---

### 5.2 Tạo mới kỹ năng (`POST /api/admin/skills`)
- **HTTP Method:** `POST`
- **URL Path:** `/api/admin/skills`
- **Request Body (SkillRequest):**
  ```json
  {
    "name": "Spring Boot"
  }
  ```

---

### 5.3 Cập nhật kỹ năng (`PUT /api/admin/skills/{id}`)
- **HTTP Method:** `PUT`
- **URL Path:** `/api/admin/skills/{id}`
- **Request Body (SkillRequest):**
  ```json
  {
    "name": "Spring Boot 3"
  }
  ```

---

### 5.4 Xóa kỹ năng (`DELETE /api/admin/skills/{id}`)
- **HTTP Method:** `DELETE`
- **URL Path:** `/api/admin/skills/{id}`

---

## 📊 6. Báo cáo Thống kê Hệ thống (Stats)

### 6.1 Tổng quan thống kê hệ thống (`GET /api/admin/stats`)
- **HTTP Method:** `GET`
- **URL Path:** `/api/admin/stats`
- **Mô tả:** Trả về các chỉ số tổng quan hệ thống: Tổng số Ứng viên, Tổng số Nhà tuyển dụng, Tổng số Công việc đang mở, Số lượng nộp hồ sơ, Báo cáo vi phạm đang chờ.

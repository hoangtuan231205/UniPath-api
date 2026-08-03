# Tài liệu API Quản trị Hệ thống (Admin API)

Tài liệu chi tiết các API dành cho Quản trị viên (Admin) bao gồm duyệt công ty mới (`AdminCompanyController`), quản lý tài khoản người dùng, xử lý báo cáo vi phạm (`reports`), và thống kê hệ thống trong **UniPath API**.

---

## 📌 Thông tin chung
- **Base URL:** `/api/admin`
- **Xác thực:** Yêu cầu Header `Authorization: Bearer <token>` có vai trò `ADMIN`.

---

## 📑 Danh sách API Duyệt Công Ty (AdminCompanyController)

### 1. Danh sách Công ty chờ duyệt (`GET /api/admin/companies`)
- **HTTP Method:** `GET`
- **URL Path:** `/api/admin/companies?status=PENDING`
- **Mô tả:** Lấy danh sách các công ty do Employer đề xuất đang chờ System Admin duyệt (Mặc định `status = PENDING`).

---

### 2. Phê duyệt Công ty (`PATCH /api/admin/companies/{id}/approve`)
- **HTTP Method:** `PATCH`
- **URL Path:** `/api/admin/companies/{id}/approve`
- **Mô tả:** System Admin chấp thuận công ty đề xuất.
- **Tác động:** Đổi trạng thái `status = 'APPROVED'`, ghi nhận `approvedBy` & `approvedAt`.

---

### 3. Từ chối Công ty (`PATCH /api/admin/companies/{id}/reject`)
- **HTTP Method:** `PATCH`
- **URL Path:** `/api/admin/companies/{id}/reject`
- **Mô tả:** System Admin từ chối công ty đề xuất.
- **Tác động:** Đổi trạng thái `status = 'REJECTED'`.

# Tài liệu API Công ty & Quản lý Đa Thành viên (Company & Multi-Employer API)

Tài liệu chi tiết các API Quản lý công ty đa thành viên (**Company Members**, **Company Join Requests**, Đề xuất công ty mới) và Quản lý ca làm/bảng lương nội bộ cho mô hình SME trong **UniPath API**.

---

## 📌 Thông tin chung
- **Base URL:** `/api/companies`, `/api/users` và `/api/employer`
- **Xác thực:** Yêu cầu Header `Authorization: Bearer <token>` có chứa token JWT.

---

## 📑 Danh sách API

### 1. Đề xuất tạo Công ty mới (`POST /api/companies`)
- **HTTP Method:** `POST`
- **URL Path:** `/api/companies`
- **Mô tả:** Employer đề xuất công ty mới. Công ty được khởi tạo với `status = 'PENDING'`. **Ngay sau khi tạo, hệ thống tự động gán người tạo làm `COMPANY_ADMIN` trong `company_members`** để họ có thể chỉnh sửa hồ sơ công ty mình tạo trong thời gian chờ System Admin duyệt.

#### Request Body (CompanyRequest)
```json
{
  "companyName": "Công ty TNHH Công Nghệ UniPath",
  "taxCode": "0109999999",
  "phoneNumber": "0987654321",
  "businessLicenseUrl": "https://example.com/license.pdf",
  "companyScale": "SME",
  "description": "Giải pháp tuyển dụng kết nối sinh viên và doanh nghiệp",
  "website": "https://unipath.vn"
}
```

---

### 2. Tìm kiếm Công ty theo tên (`GET /api/companies/search`)
- **HTTP Method:** `GET`
- **URL Path:** `/api/companies/search?keyword=UniPath`
- **Mô tả:** Employer tìm kiếm công ty đã tồn tại trước khi tạo mới hoặc gửi yêu cầu gia nhập. **Chỉ trả về các công ty có `status = 'APPROVED'`**.

---

### 3. Chi tiết Công ty (`GET /api/companies/{id}`)
- **HTTP Method:** `GET`
- **URL Path:** `/api/companies/{id}`
- **Mô tả:** Xem thông tin công ty.
  - Công khai cho tất cả nếu `status = 'APPROVED'`.
  - Nếu `status = 'PENDING'` hoặc `'REJECTED'`: Chỉ người tạo/thành viên trong `company_members` hoặc System Admin mới có quyền xem (Nếu không có quyền trả lỗi 403 Forbidden).

---

### 4. Cập nhật hồ sơ Công ty (`PUT /api/companies/{id}`)
- **HTTP Method:** `PUT`
- **URL Path:** `/api/companies/{id}`
- **Phân quyền:** Chỉ người dùng có vai trò `COMPANY_ADMIN` trong `company_members` của công ty đó mới được phép chỉnh sửa. Ngược lại trả lỗi `403 Forbidden`.

---

### 5. Danh sách Công ty của tôi (`GET /api/users/me/companies`)
- **HTTP Method:** `GET`
- **URL Path:** `/api/users/me/companies`
- **Mô tả:** Trả về danh sách tất cả các công ty mà người dùng hiện tại đang làm thành viên kèm vai trò (`COMPANY_ADMIN` hoặc `RECRUITER`) và thời gian gia nhập.

---

### 6. Gửi yêu cầu gia nhập Công ty (`POST /api/companies/{id}/join-requests`)
- **HTTP Method:** `POST`
- **URL Path:** `/api/companies/{id}/join-requests`
- **Mô tả:** Employer gửi yêu cầu xin gia nhập 1 công ty đã `APPROVED`.
- **Ràng buộc:** Validate chưa có yêu cầu `PENDING` trùng (`userId`, `companyId`).

#### Request Body (CompanyJoinRequestDTO)
```json
{
  "message": "Tôi là HR mới xin gia nhập công ty để đăng tin tuyển dụng"
}
```

---

### 7. Xem danh sách yêu cầu gia nhập (`GET /api/companies/{id}/join-requests`)
- **HTTP Method:** `GET`
- **URL Path:** `/api/companies/{id}/join-requests?status=PENDING`
- **Phân quyền:** Chỉ `COMPANY_ADMIN` của công ty đó hoặc System Admin (`role = 'ADMIN'`) mới được xem.

---

### 8. Duyệt yêu cầu gia nhập Công ty (`PATCH /api/join-requests/{id}/approve`)
- **HTTP Method:** `PATCH`
- **URL Path:** `/api/join-requests/{id}/approve`
- **Phân quyền người duyệt:**
  - Nếu công ty đã có ít nhất 1 `COMPANY_ADMIN` -> Chỉ `COMPANY_ADMIN` đó được duyệt.
  - Nếu công ty chưa có `COMPANY_ADMIN` nào -> Chỉ System Admin (`role = 'ADMIN'`) được duyệt.
- **Tác động:** Set status `APPROVED` và chèn bản ghi vào `company_members` với `member_role = 'RECRUITER'`.

---

### 9. Từ chối yêu cầu gia nhập Công ty (`PATCH /api/join-requests/{id}/reject`)
- **HTTP Method:** `PATCH`
- **URL Path:** `/api/join-requests/{id}/reject`
- **Tác động:** Set status `REJECTED` cho request gia nhập. Phân quyền áp dụng tương tự API approve.

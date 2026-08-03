# 📚 Tài liệu API Hệ thống UniPath (UniPath API Documentation)

Chào mừng bạn đến với tài liệu API chính thức của dự án **UniPath API**.

---

## 📂 Danh mục Tài liệu API Chi tiết

Tài liệu được chia thành các file riêng biệt tương ứng với từng phân hệ (Controller) của ứng dụng:

1. 📘 **[Tài liệu Tổng hợp Tất cả API (FULL_API_DOCUMENTATION.md)](./FULL_API_DOCUMENTATION.md)** *(File tổng hợp toàn bộ API)*
2. 🔑 **[Tài liệu API Xác thực & Phân quyền (AUTH_API.md)](./AUTH_API.md)**
3. 🗺️ **[Tài liệu API Vị trí Công ty (COMPANY_LOCATION_API.md)](./COMPANY_LOCATION_API.md)**
4. 👤 **[Tài liệu API Hồ sơ Người dùng (PROFILE_API.md)](./PROFILE_API.md)**
5. 💼 **[Tài liệu API Tin Tuyển dụng (JOB_API.md)](./JOB_API.md)**
6. 📝 **[Tài liệu API Bài viết Cộng đồng & Feed (COMMUNITY_POST_API.md)](./COMMUNITY_POST_API.md)**
7. ❤️ **[Tài liệu API Tương tác Like, Comment, Share (INTERACTION_API.md)](./INTERACTION_API.md)**
8. 📄 **[Tài liệu API Quản lý Ứng tuyển & CV (APPLICATION_API.md)](./APPLICATION_API.md)**
9. ⭐ **[Tài liệu API Lưu Tin Tuyển dụng (SAVED_JOB_API.md)](./SAVED_JOB_API.md)**
10. 🤖 **[Tài liệu API Mẫu CV AI (CV_PROFILE_API.md)](./CV_PROFILE_API.md)**
11. 🏢 **[Tài liệu API Công ty & Quản lý SME (COMPANY_MANAGEMENT_API.md)](./COMPANY_MANAGEMENT_API.md)**
12. 🔍 **[Tài liệu API Tìm kiếm Ứng viên (CANDIDATE_SEARCH_API.md)](./CANDIDATE_SEARCH_API.md)**
13. 🛡️ **[Tài liệu API Quản trị Hệ thống (ADMIN_API.md)](./ADMIN_API.md)**
14. 🔔 **[Tài liệu API Thông báo (NOTIFICATION_API.md)](./NOTIFICATION_API.md)**

---

## ⚙️ Quy chuẩn chung

### 🌐 Format Request/Response
- **Content-Type:** `application/json` (riêng nộp CV hỗ trợ `multipart/form-data`)
- **Mã hóa:** UTF-8

### 🛡️ Xác thực (Authentication)
Dự án sử dụng **JSON Web Token (JWT)**. Đối với các API yêu cầu xác thực, đính kèm Header:
```http
Authorization: Bearer <your_jwt_token>
```

### 🔴 Các Mã Trạng Thái HTTP Thường Gặp
- `200 OK`: Xử lý thành công.
- `400 Bad Request`: Dữ liệu không hợp lệ hoặc vi phạm quy tắc nghiệp vụ.
- `401 Unauthorized`: Chưa xác thực hoặc Token không hợp lệ / hết hạn.
- `403 Forbidden`: Không có quyền thao tác trên tài nguyên.
- `404 Not Found`: Không tìm thấy tài nguyên.
- `500 Internal Server Error`: Lỗi hệ thống server.

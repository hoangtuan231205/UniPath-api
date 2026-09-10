# 📚 Tài liệu API Hệ thống UniPath (UniPath API Documentation)

Chào mừng bạn đến với tài liệu API chính thức của dự án **UniPath API**.

---

## 📂 Danh mục Tất cả các File Tài liệu API

Tài liệu được phân chia thành từng file Markdown độc lập tương ứng với các Controller và Phân hệ chức năng của ứng dụng:

1. 👑 **[Tài liệu API Quản trị Hệ thống (ADMIN_API.md)](./ADMIN_API.md)**
   - Phê duyệt công ty đề xuất (`/api/admin/companies`)
   - Quản lý tài khoản người dùng (`/api/admin/users`), Khóa/Mở khóa tài khoản
   - Hệ thống báo cáo vi phạm (`/api/admin/reports`, `/api/jobs/{id}/report`)
   - Quản lý danh mục (`/api/admin/categories`) & Kỹ năng (`/api/admin/skills`)
   - Thống kê tổng quan hệ thống (`/api/admin/stats`)

2. 📄 **[Tài liệu API Ứng tuyển & File CV (APPLICATION_API.md)](./APPLICATION_API.md)**
   - Nộp hồ sơ ứng tuyển kèm upload file CV (`POST /api/applications`)
   - Rút hồ sơ, xem lịch sử ứng tuyển (`GET /api/applications/me`)
   - Nhà tuyển dụng duyệt ứng viên, đổi trạng thái & tải file CV (`GET /api/applications/{id}/cv/download`)

3. 🔑 **[Tài liệu API Xác thực & Phân quyền (AUTH_API.md)](./AUTH_API.md)**
   - Đăng ký tài khoản (`POST /api/auth/register`)
   - Đăng nhập & lấy Token JWT (`POST /api/auth/login`)

4. 🔍 **[Tài liệu API Tìm kiếm Ứng viên (CANDIDATE_SEARCH_API.md)](./CANDIDATE_SEARCH_API.md)**
   - Tìm kiếm ứng viên dành cho nhà tuyển dụng theo từ khóa, kỹ năng (`GET /api/candidates/search`)

5. 💬 **[Tài liệu API Bài viết Cộng đồng (COMMUNITY_POST_API.md)](./COMMUNITY_POST_API.md)**
   - Tạo bài viết, đăng ảnh, chỉnh sửa, xóa bài viết cộng đồng (`/api/community/posts`)

6. 🗺️ **[Tài liệu API Vị trí Công ty (COMPANY_LOCATION_API.md)](./COMPANY_LOCATION_API.md)**
   - Thêm vị trí tọa độ địa lý cho công ty (`POST /api/companies/add`)
   - Tìm kiếm công ty trong bán kính xung quanh vị trí (`GET /api/companies/nearby`)

7. 🏢 **[Tài liệu API Quản lý Doanh nghiệp (COMPANY_MANAGEMENT_API.md)](./COMPANY_MANAGEMENT_API.md)**
   - Xem danh sách công ty, tạo công ty mới, cập nhật thông tin doanh nghiệp (`/api/companies`)

8. 📝 **[Tài liệu API Hồ sơ CV Mẫu (CV_PROFILE_API.md)](./CV_PROFILE_API.md)**
   - Quản lý các mẫu CV cá nhân tạo trên hệ thống (`/api/cv-profiles`)

9. 🗓️ **[Tài liệu API Ca làm việc & Hợp đồng Nhân viên (EMPLOYMENT_API.md)](./EMPLOYMENT_API.md)**
   - Lịch ca làm việc hôm nay, phân ca mới, sửa ca (`/api/employer/shifts`)
   - Quản lý danh sách nhân viên công ty (`/api/employer/employees`)
   - Tính toán bảng lương theo tháng (`/api/employer/payroll`)

10. 📰 **[Tài liệu API Bảng tin Cộng đồng (FEED_API.md)](./FEED_API.md)**
    - Lấy bảng tin tổng hợp trộn lẫn bài viết & tin tuyển dụng phân trang dạng Infinite Scroll (`GET /api/feed`)

11. ❤️ **[Tài liệu API Tương tác Bài viết (INTERACTION_API.md)](./INTERACTION_API.md)**
    - Thích bài viết, bình luận, báo cáo bài viết vi phạm (`/api/community/posts/{id}/like`, `/comments`)

12. 💼 **[Tài liệu API Tin Tuyển dụng (JOB_API.md)](./JOB_API.md)**
    - Tạo, chỉnh sửa, đóng tin tuyển dụng, tìm kiếm việc làm nâng cao theo địa điểm/lương/ngành nghề (`/api/jobs`)

13. 🔔 **[Tài liệu API Thông báo (NOTIFICATION_API.md)](./NOTIFICATION_API.md)**
    - Xem danh sách thông báo cá nhân, đánh giá đã đọc (`/api/notifications`)

14. 👤 **[Tài liệu API Hồ sơ Người dùng (PROFILE_API.md)](./PROFILE_API.md)**
    - Hồ sơ Ứng viên (`/api/profiles/candidate/{userId}`) & Hồ sơ Nhà tuyển dụng (`/api/profiles/employer/{userId}`)

15. 🔖 **[Tài liệu API Việc làm đã lưu (SAVED_JOB_API.md)](./SAVED_JOB_API.md)**
    - Lưu tin tuyển dụng yêu thích, bỏ lưu, xem danh sách việc làm đã lưu (`/api/saved-jobs`)

16. 👤 **[Tài liệu API Thông tin Người dùng (USER_API.md)](./USER_API.md)**
    - Lấy danh sách công ty do người dùng hiện tại quản lý (`GET /api/users/me/companies`)

---

## ⚙️ Quy chuẩn chung

### 🌐 Format Request/Response
- **Content-Type:** `application/json`
- **Mã hóa:** UTF-8

### 🛡️ Xác thực (Authentication)
Dự án sử dụng **JSON Web Token (JWT)**.
Đối với các API yêu cầu xác thực, hãy đính kèm Header:
```http
Authorization: Bearer <your_jwt_token>
```

### 🔴 Các Mã Trạng Thái HTTP Thường Gặp
- `200 OK`: Xử lý yêu cầu thành công.
- `400 Bad Request`: Dữ liệu gửi lên không hợp lệ hoặc vi phạm quy tắc nghiệp vụ.
- `401 Unauthorized`: Chưa xác thực hoặc Token không hợp lệ / hết hạn.
- `403 Forbidden`: Không có quyền truy cập API.
- `500 Internal Server Error`: Lỗi hệ thống server.

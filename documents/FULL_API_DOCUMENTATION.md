# 📘 Tài liệu Tổng hợp Tất cả API Dự án UniPath (Full API Documentation)

Tài liệu tổng hợp **toàn bộ 13 phân hệ API** hiện tại của dự án **UniPath API** (bao gồm các API ban đầu và tất cả các API mới được phát triển).

---

## 🌐 Quy chuẩn chung

### 🛡️ Xác thực (Authentication)
Dự án sử dụng **JSON Web Token (JWT)**. Đối với các API yêu cầu xác thực, hãy đính kèm Header:
```http
Authorization: Bearer <your_jwt_token>
```

### 🔴 HTTP Response Format
Mọi endpoint trả về `ResponseEntity<?>` bọc trong try/catch:
- `200 OK`: Xử lý thành công.
- `400 Bad Request`: Lỗi dữ liệu đầu vào hoặc nghiệp vụ (`e.getMessage()`).
- `401 Unauthorized`: Chưa đăng nhập hoặc Token hết hạn.
- `403 Forbidden`: Không có quyền truy cập tài nguyên.
- `404 Not Found`: Không tìm thấy dữ liệu.

---

## 📑 Danh mục Phân hệ API & Đường dẫn chi tiết

| STT | Phân hệ API | Base URL | Mô tả chính | File Tài liệu Chi tiết |
| :---: | :--- | :--- | :--- | :--- |
| **1** | **Xác thực & Phân quyền** | `/api/auth` | Đăng ký tài khoản (Candidate/Employer) & Đăng nhập lấy Token JWT. | [AUTH_API.md](./AUTH_API.md) |
| **2** | **Vị trí Địa lý Công ty** | `/api/companies` | Thêm tọa độ PostGIS & Tìm kiếm công ty lân cận theo bán kính. | [COMPANY_LOCATION_API.md](./COMPANY_LOCATION_API.md) |
| **3** | **Hồ sơ Người dùng** | `/api/profiles` | Lấy và cập nhật hồ sơ chi tiết cho Candidate và Employer. | [PROFILE_API.md](./PROFILE_API.md) |
| **4** | **Tin Tuyển dụng (Jobs)** | `/api/jobs` | CRUD tin tuyển dụng, đóng tin, đếm tương tác, Feed phân trang cursor. | [JOB_API.md](./JOB_API.md) |
| **5** | **Bài viết Cộng đồng** | `/api/posts`, `/api/feed` | Đăng/sửa/xoá bài viết & Feed tổng hợp gộp Jobs + Posts theo thời gian. | [COMMUNITY_POST_API.md](./COMMUNITY_POST_API.md) |
| **6** | **Tương tác (Interaction)** | `/api/jobs`, `/api/posts` | Like/Unlike, Bình luận (hỗ trợ reply lồng nhau), Chia sẻ tin tuyển dụng. | [INTERACTION_API.md](./INTERACTION_API.md) |
| **7** | **Ứng tuyển (Applications)** | `/api/applications` | Nộp hồ sơ (upload CV file <= 5MB PDF/DOCX), duyệt hồ sơ, tải file CV. | [APPLICATION_API.md](./APPLICATION_API.md) |
| **8** | **Tin tuyển dụng Đã lưu** | `/api/saved-jobs` | Lưu/Bỏ lưu tin tuyển dụng yêu thích của Candidate. | [SAVED_JOB_API.md](./SAVED_JOB_API.md) |
| **9** | **CV AI (User CV Profiles)** | `/api/cv-profiles` | Quản lý mẫu CV do AI khởi tạo & Thiết lập CV mặc định. | [CV_PROFILE_API.md](./CV_PROFILE_API.md) |
| **10** | **Công ty & SME Nội bộ** | `/api/companies`, `/api/employer` | Quản lý profile công ty, ca làm việc hôm nay, danh sách nhân viên & bảng lương SME. | [COMPANY_MANAGEMENT_API.md](./COMPANY_MANAGEMENT_API.md) |
| **11** | **Tìm kiếm Ứng viên** | `/api/candidates` | Employer tìm kiếm hồ sơ ứng viên theo kỹ năng, chuyên ngành, trường. | [CANDIDATE_SEARCH_API.md](./CANDIDATE_SEARCH_API.md) |
| **12** | **Quản trị Viên (Admin)** | `/api/admin` | Quản lý user (chặn ban admin), duyệt report vi phạm, CRUD danh mục/kỹ năng, thống kê stats. | [ADMIN_API.md](./ADMIN_API.md) |
| **13** | **Thông báo (Notification)** | `/api/notifications` | Danh sách thông báo ứng tuyển/hệ thống & Đánh dấu đã đọc. | [NOTIFICATION_API.md](./NOTIFICATION_API.md) |

---

## 📌 Tóm tắt Danh sách Endpoint Toàn dự án

### 🔑 1. Auth & Profiles (`/api/auth`, `/api/profiles`)
- `POST /api/auth/register` — Đăng ký tài khoản
- `POST /api/auth/login` — Đăng nhập
- `GET /api/profiles/candidate/{userId}` — Lấy hồ sơ ứng viên
- `PUT /api/profiles/candidate/{userId}` — Cập nhật hồ sơ ứng viên
- `GET /api/profiles/employer/{userId}` — Lấy hồ sơ nhà tuyển dụng
- `PUT /api/profiles/employer/{userId}` — Cập nhật hồ sơ nhà tuyển dụng

### 💼 2. Jobs & Community (`/api/jobs`, `/api/posts`, `/api/feed`)
- `POST /api/jobs` — Tạo tin tuyển dụng mới (`isActive = false`)
- `GET /api/jobs/{id}` — Chi tiết tin tuyển dụng & counter
- `PUT /api/jobs/{id}` — Sửa tin tuyển dụng (chỉ chủ sở hữu)
- `PATCH /api/jobs/{id}/close` — Đóng tin tuyển dụng
- `DELETE /api/jobs/{id}` — Xoá tin tuyển dụng (khi chưa có người ứng tuyển)
- `GET /api/jobs/feed?cursor=` — Danh sách tin tuyển dụng phân trang cursor
- `POST /api/posts` — Tạo bài viết cộng đồng
- `GET /api/posts/{id}`, `PUT /api/posts/{id}`, `DELETE /api/posts/{id}` — CRUD bài viết
- `GET /api/feed?cursor=` — Feed gộp cả Jobs & Posts theo thời gian (`type: "JOB" | "POST"`)

### ❤️ 3. Interactions & Saved Jobs (`/api/jobs`, `/api/posts`, `/api/saved-jobs`)
- `POST /api/jobs/{id}/like` & `DELETE /api/jobs/{id}/like` — Like/Unlike Job
- `POST /api/posts/{id}/like` & `DELETE /api/posts/{id}/like` — Like/Unlike Post
- `GET /api/jobs/{id}/comments`, `POST /api/jobs/{id}/comments`, `DELETE /api/comments/{id}` — Bình luận Job (hỗ trợ `parentCommentId`)
- `GET /api/posts/{id}/comments`, `POST /api/posts/{id}/comments`, `DELETE /api/posts/comments/{id}` — Bình luận Post
- `POST /api/jobs/{id}/share` — Chia sẻ Job
- `POST /api/jobs/{id}/save` & `DELETE /api/jobs/{id}/save` — Lưu/Bỏ lưu Job
- `GET /api/saved-jobs` — Danh sách Job đã lưu

### 📄 4. Applications & CV Profiles (`/api/applications`, `/api/cv-profiles`)
- `GET /api/jobs/{id}/check-applied` — Kiểm tra trạng thái ứng tuyển
- `POST /api/applications` — Nộp hồ sơ ứng tuyển (chặn trùng, upload file CV <= 5MB PDF/DOCX)
- `DELETE /api/applications/{id}` — Rút hồ sơ (khi status = SUBMITTED)
- `GET /api/applications/me` — Lịch sử ứng tuyển của Ứng viên
- `GET /api/jobs/{id}/applications` — Danh sách ứng viên theo Job (cho Nhà tuyển dụng)
- `PATCH /api/applications/{id}/status` — Đổi trạng thái ứng tuyển & tự động gửi thông báo
- `GET /api/applications/{id}/cv/download` — Tải file CV
- `GET /api/cv-profiles`, `POST /api/cv-profiles`, `PUT /api/cv-profiles/{id}`, `DELETE /api/cv-profiles/{id}` — CRUD CV AI
- `PATCH /api/cv-profiles/{id}/set-primary` — Đặt làm CV chính

### 🏢 5. Company & SME Management (`/api/companies`, `/api/employer`)
- `POST /api/companies/add` — Thêm vị trí địa lý công ty (PostGIS lat/lon)
- `GET /api/companies/nearby` — Tìm kiếm công ty lân cận theo bán kính
- `GET /api/companies/me`, `PUT /api/companies/me` — Lấy & cập nhật thông tin công ty (trả về `companyScale`)
- `GET /api/employer/shifts/today` — Danh sách ca làm hôm nay (dành cho SME)
- `POST /api/employer/shifts`, `PUT /api/employer/shifts/{id}` — Tạo/Sửa ca làm
- `GET /api/employer/employees` — Danh sách nhân viên nội bộ
- `GET /api/employer/payroll` — Xem bảng lương nội bộ

### 🔍 6. Candidate Search & Notifications (`/api/candidates`, `/api/notifications`)
- `GET /api/candidates/search?skill=&major=&university=&keyword=` — Tìm kiếm ứng viên
- `GET /api/notifications` — Danh sách thông báo
- `PATCH /api/notifications/{id}/read` — Đánh dấu 1 thông báo đã đọc
- `PATCH /api/notifications/read-all` — Đánh dấu tất cả thông báo đã đọc

### 🛡️ 7. Admin (`/api/admin`)
- `GET /api/admin/users?type=&status=&search=` — Danh sách người dùng
- `PATCH /api/admin/users/{id}/ban` & `PATCH /api/admin/users/{id}/unban` — Khóa/Mở khóa tài khoản (chặn ban ADMIN)
- `POST /api/jobs/{id}/report` — Candidate gửi báo cáo tin vi phạm
- `GET /api/admin/reports` & `PATCH /api/admin/reports/{id}/resolve` — Xem & xử lý báo cáo (`REJECT`, `DELETE_POST`, `BAN_ACCOUNT`)
- `GET/POST/PUT/DELETE /api/admin/categories` — CRUD Danh mục ngành nghề
- `GET/POST/PUT/DELETE /api/admin/skills` — CRUD Kỹ năng (kiểm tra tham chiếu `job_skills`)
- `GET /api/admin/stats` — Thống kê tổng quan hệ thống (Candidates, Employers, Active Jobs, Status counts)

# 🚀 UniPath API — Nền Tảng Tuyển Dụng, Việc Làm Sinh Viên & Mạng Xã Hội Nghề Nghiệp

<p align="center">
  <img src="https://img.shields.io/badge/Java-21%20%7C%2025-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java" />
  <img src="https://img.shields.io/badge/Spring%20Boot-4.0.5-6DB33F?style=for-the-badge&logo=springboot&logoColor=white" alt="Spring Boot" />
  <img src="https://img.shields.io/badge/PostgreSQL-15+-4169E1?style=for-the-badge&logo=postgresql&logoColor=white" alt="PostgreSQL" />
  <img src="https://img.shields.io/badge/PostGIS-Spatial%20GIS-336791?style=for-the-badge&logo=postgresql&logoColor=white" alt="PostGIS" />
  <img src="https://img.shields.io/badge/JWT-Authentication-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white" alt="JWT" />
  <img src="https://img.shields.io/badge/Maven-Build%20Tool-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white" alt="Maven" />
</p>

---

## 📖 1. Giới thiệu dự án (Overview)

**UniPath API** là hệ thống Backend dịch vụ cốt lõi (Core Backend RESTful API) cho nền tảng **UniPath** — Hệ sinh thái kết nối việc làm, thực tập và định hướng nghề nghiệp dành cho sinh viên và các trường đại học với các doanh nghiệp.

Không chỉ dừng lại ở các tính năng tuyển dụng truyền thống, UniPath API tích hợp:
- **Tìm kiếm theo định vị không gian (GIS / PostGIS):** Tìm kiếm việc làm và doanh nghiệp xung quanh vị trí thực tế của ứng viên.
- **Mạng xã hội nghề nghiệp & Bảng tin thông minh (Community Feed):** Cơ chế Infinite Scroll trộn lẫn bài thảo luận chuyên môn và tin tuyển dụng nổi bật.
- **Quản lý ca làm & Tính lương (Shifts & Payroll):** Hỗ trợ doanh nghiệp quản lý lịch làm việc theo ca, theo dõi nhân viên part-time/intern và tính lương tự động hàng tháng.
- **Bảo mật & Kiểm duyệt chuyên sâu:** Cơ chế kiểm duyệt tin tuyển dụng, xác thực pháp lý doanh nghiệp, bảo vệ file CV cá nhân trong thư mục phân quyền riêng.

---

## 🛠️ 2. Công nghệ sử dụng (Tech Stack)

| Thành phần | Công nghệ / Thư viện | Ghi chú |
| :--- | :--- | :--- |
| **Ngôn ngữ** | Java 21 / 25 | Sử dụng các tính năng hiện đại của Java LTS |
| **Framework** | Spring Boot 4.0.5 | Web MVC, Data JPA, Validation, DevTools |
| **Cơ sở dữ liệu** | PostgreSQL 15+ | Kèm mở rộng **PostGIS** xử lý không gian địa lý |
| **ORM / Spatial** | Hibernate ORM 6.x + Hibernate Spatial | Ánh xạ thực thể và dữ liệu tọa độ `Geometry(Point)` |
| **Xác thực & Bảo mật** | JWT (jjwt 0.11.5) & Role-Based Access Control | Phân quyền `ADMIN`, `EMPLOYER`, `CANDIDATE` |
| **Quản lý Dependencies** | Maven 3.9+ | Wrapper `mvnw` đi kèm dự án |
| **Lưu trữ tệp tin** | Local Storage System | `uploads/` (công khai ảnh) & `private-uploads/` (bảo mật CV) |

---

## 🌟 3. Các phân hệ chức năng chính (Core Modules)

### 🔑 1. Xác thực & Phân quyền (Auth & Security)
- Đăng ký, đăng nhập cấp phát **JSON Web Token (JWT)**.
- Phân quyền theo vai trò: `ADMIN`, `EMPLOYER`, `CANDIDATE`.
- Đổi mật khẩu bảo mật và cơ chế tự động ngắt phiên/chặn yêu cầu nếu tài khoản bị khóa (`validateActiveUser`).

### 🏢 2. Doanh nghiệp & Đa thành viên (Company & Organization)
- Doanh nghiệp đề xuất tạo hồ sơ công ty và chờ Quản trị viên duyệt (`status = PENDING -> APPROVED`).
- Cơ chế quản lý tổ chức: Chủ sở hữu (`COMPANY_ADMIN`), quản lý lời mời xin gia nhập công ty (`company_join_requests`).
- Tích hợp tọa độ địa lý GPS, tìm kiếm doanh nghiệp lân cận bán kính $R$ km qua PostGIS.

### 💼 3. Quản lý Tuyển dụng & Tìm kiếm (Jobs)
- Đăng tin, cập nhật, đóng/mở tin tuyển dụng, phân loại ngành nghề (`job_categories`) và kỹ năng (`skills`).
- Bộ lọc tìm kiếm nâng cao: Từ khóa, mức lương, kinh nghiệm, địa điểm, khoảng cách tọa độ.
- Lưu việc làm quan tâm (`Saved Jobs`).

### 📄 4. Hồ sơ Ứng tuyển & Lưu trữ CV (Applications & CV Storage)
- Ứng viên nộp đơn ứng tuyển đính kèm CV (File PDF/DOCX tối đa dung lượng quy định).
- Hệ thống lưu trữ bảo mật riêng biệt (`private-uploads`), ngăn chặn tải trái phép nếu không phải HR của công ty hoặc chính ứng viên.
- Nhà tuyển dụng cập nhật tiến trình hồ sơ: `PENDING` ➔ `REVIEWED` ➔ `ACCEPTED` / `REJECTED`.
- Hỗ trợ ứng viên rút hồ sơ ứng tuyển khi chưa xét duyệt.

### 🗓️ 5. Quản lý Nhân sự, Ca làm & Bảng lương (Employment & Payroll)
- Quản lý danh sách nhân viên nội bộ của doanh nghiệp.
- Thiết lập lịch ca làm việc (Shifts) hôm nay, phân ca cho nhân viên.
- Bảng chấm công và tự động tổng hợp bảng lương dự kiến (`payroll`) theo tháng.

### 💬 6. Mạng xã hội Nghề nghiệp & Tương tác (Community & Social Feed)
- Đăng bài viết chia sẻ kinh nghiệm, đính kèm nhiều hình ảnh.
- Thích (Like), bình luận (Comment), báo cáo vi phạm bài viết.
- **Bảng tin tổng hợp (Unified Feed):** Thuật toán phân trang Infinite Scroll đan xen bài thảo luận cộng đồng và tin tuyển dụng hấp dẫn.

### 👑 7. Cổng Quản trị Hệ thống (Admin Portal)
- Phê duyệt / Từ chối hồ sơ doanh nghiệp mới.
- Quản lý trạng thái tài khoản người dùng (Khóa / Mở khóa tài khoản ngay lập tức).
- Tiếp nhận và xử lý báo cáo vi phạm (Job reports, Community reports).
- Quản lý từ điển dữ liệu hệ thống: Danh mục nghề nghiệp, Danh sách kỹ năng chuẩn hóa.
- Ghi vết lịch sử thao tác (`Admin Audit Logs`) và Thống kê tổng quan hệ thống (`Dashboard Stats`).

---

## 📁 4. Cấu trúc thư mục mã nguồn (Project Structure)

```text
UniPath-api/
├── .github/                       # GitHub Actions & workflows
├── documents/                     # 📚 TOÀN BỘ TÀI LIỆU KỸ THUẬT & API
│   ├── ADMIN_API.md               # API Quản trị hệ thống
│   ├── APPLICATION_API.md         # API Ứng tuyển & CV
│   ├── AUTH_API.md                # API Xác thực JWT
│   ├── CANDIDATE_SEARCH_API.md    # API Tìm kiếm ứng viên
│   ├── COMMUNITY_POST_API.md      # API Bài viết cộng đồng
│   ├── COMPANY_LOCATION_API.md    # API Vị trí địa lý PostGIS
│   ├── COMPANY_MANAGEMENT_API.md  # API Doanh nghiệp
│   ├── CV_PROFILE_API.md          # API Hồ sơ CV mẫu
│   ├── EMPLOYMENT_API.md          # API Ca làm việc & Tính lương
│   ├── FEED_API.md                # API Bảng tin Infinite Scroll
│   ├── INTERACTION_API.md         # API Like, Bình luận, Report
│   ├── JOB_API.md                 # API Tin tuyển dụng
│   ├── NOTIFICATION_API.md        # API Thông báo người dùng
│   ├── POSTMAN_TEST_GUIDE.md      # 🔥 Hướng dẫn toàn diện test qua Postman
│   ├── PROFILE_API.md             # API Hồ sơ Candidate / Employer
│   ├── SAVED_JOB_API.md           # API Lưu việc làm
│   ├── USER_API.md                # API Thông tin người dùng
│   └── uml/                       # Sơ đồ Use Case & Thiết kế hệ thống
├── uploads/                       # Lưu trữ file công khai (ảnh bài viết, avatar)
├── private-uploads/               # Lưu trữ file CV bảo mật
├── src/
│   └── main/
│       ├── java/com/example/unipathapi/
│       │   ├── admin/             # Quản trị viên (Reports, Approval, Audit, Stats)
│       │   ├── application/       # Hồ sơ ứng tuyển & File Storage
│       │   ├── auth/              # Đăng ký, Đăng nhập, Token Handler
│       │   ├── candidate/         # Hồ sơ & Tìm kiếm ứng viên
│       │   ├── common/            # Cấu hình Web, SecurityUtil, JwtUtil, Exception
│       │   ├── community/         # Bài viết cộng đồng, Like, Comment
│       │   ├── company/           # Quản lý Doanh nghiệp, Vị trí PostGIS
│       │   ├── cv/                # Mẫu CV cá nhân
│       │   ├── employer/          # Hồ sơ NTD, Ca làm việc, Chấm công & Lương
│       │   ├── job/               # Tin tuyển dụng & Việc làm đã lưu
│       │   ├── notification/      # Thông báo hệ thống
│       │   ├── user/              # Quản lý thực thể Người dùng
│       │   └── UniPathApiApplication.java
│       └── resources/
│           ├── application.properties
│           └── static/
├── pom.xml                        # Cấu hình Maven dependencies
└── README.md
```

---

## ⚡ 5. Hướng dẫn cài đặt & Khởi chạy (Getting Started)

### ⚙️ Yêu cầu môi trường (Prerequisites)
- **Java Development Kit (JDK):** Phiên bản 21 hoặc 25.
- **PostgreSQL:** Phiên bản 14 trở lên.
- **PostGIS Extension:** Đã cài đặt tiện ích mở rộng PostGIS cho PostgreSQL.

---

### 🗄️ Bước 1: Khởi tạo Cơ sở dữ liệu PostgreSQL & PostGIS

1. Mở công cụ quản lý PostgreSQL (như pgAdmin, DBeaver hoặc `psql`):
```sql
-- 1. Tạo database mới
CREATE DATABASE "UniPath";

-- 2. Kết nối vào database UniPath và bật extension PostGIS
\c UniPath;
CREATE EXTENSION IF NOT EXISTS postgis;
```

---

### ⚙️ Bước 2: Cấu hình thông tin kết nối

Mở file [`src/main/resources/application.properties`](file:///c:/Users/Admin/Documents/UniPath-api/src/main/resources/application.properties) và điều chỉnh thông số cho phù hợp với máy của bạn:

```properties
# Tên ứng dụng
spring.application.name=UniPath-api
server.port=8080

# Cấu hình kết nối PostgreSQL
spring.datasource.url=jdbc:postgresql://localhost:5432/UniPath
spring.datasource.username=postgres
spring.datasource.password=your_password
spring.datasource.driver-class-name=org.postgresql.Driver

# Cấu hình Hibernate / JPA (Tự động sinh bảng)
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect

# Cấu hình Upload File
spring.servlet.multipart.max-file-size=5MB
spring.servlet.multipart.max-request-size=20MB
```

---

### 🚀 Bước 3: Chạy ứng dụng

Mở terminal tại thư mục gốc của dự án:

#### Dùng Maven Wrapper trên Windows (PowerShell / CMD):
```bash
.\mvnw.cmd clean spring-boot:run
```

#### Dùng Maven Wrapper trên macOS / Linux:
```bash
./mvnw clean spring-boot:run
```

Khi console hiển thị:
```text
Started UniPathApiApplication in X.XXX seconds (process running for X.XXX)
```
Backend đã khởi chạy thành công tại địa chỉ: `http://localhost:8080`.

---

### 🔑 Tài khoản Quản trị viên Mặc định (Default Superadmin)
Khi hệ thống khởi chạy, lớp `SequenceFixInitializer` / `UniPathApiApplication` sẽ tự động cấu hình hoặc đồng bộ tài khoản SuperAdmin:
- **Email:** `superadmin@unipath.local`
- **Mật khẩu:** `superadmin`
- **Vai trò (Role):** `ADMIN`

---

## 📡 6. Chuẩn chuẩn tích hợp API (API Guidelines)

- **Đường dẫn gốc:** `http://localhost:8080/api`
- **Định dạng dữ liệu:** `Content-Type: application/json` (UTF-8)

### 🛡️ Cơ chế Xác thực (JWT Header)
Các endpoint yêu cầu quyền đăng nhập cần đính kèm JWT vào header:
```http
Authorization: Bearer <your_jwt_access_token>
```

### 🔴 Các mã HTTP Status phổ biến
- `200 OK`: Thực hiện thành công.
- `201 Created`: Tạo mới bản ghi thành công.
- `400 Bad Request`: Thiếu tham số hoặc dữ liệu không hợp lệ.
- `401 Unauthorized`: Chưa truyền Token hoặc Token đã hết hạn.
- `403 Forbidden`: Tài khoản bị khóa hoặc không đủ quyền truy cập tài nguyên.
- `404 Not Found`: Không tìm thấy dữ liệu yêu cầu.
- `500 Internal Server Error`: Lỗi xử lý logic từ phía máy chủ.

---

## 📚 7. Tài liệu API chi tiết & Postman Guide

Chi tiết đầy đủ tham số request, body JSON, response mẫu cho từng endpoint được lưu trữ tại thư mục [`documents/`](file:///c:/Users/Admin/Documents/UniPath-api/documents/):

| STT | Tài liệu API | Nội dung chính |
| :-: | :--- | :--- |
| 🔥 | **[POSTMAN_TEST_GUIDE.md](documents/POSTMAN_TEST_GUIDE.md)** | **Tài liệu toàn diện hướng dẫn test tuần tự 13 phân hệ bằng Postman** |
| 1 | **[AUTH_API.md](documents/AUTH_API.md)** | Đăng ký, đăng nhập, cấp JWT Token, đổi mật khẩu |
| 2 | **[ADMIN_API.md](documents/ADMIN_API.md)** | Phê duyệt công ty, khóa/mở khóa tài khoản, xử lý báo cáo, thống kê |
| 3 | **[JOB_API.md](documents/JOB_API.md)** | Đăng tuyển, lọc việc làm đa tiêu chí, đóng tin |
| 4 | **[APPLICATION_API.md](documents/APPLICATION_API.md)** | Nộp CV, quản lý trạng thái ứng tuyển, tải CV bảo mật |
| 5 | **[COMPANY_MANAGEMENT_API.md](documents/COMPANY_MANAGEMENT_API.md)** | Tạo công ty, xin gia nhập công ty, duyệt thành viên |
| 6 | **[COMPANY_LOCATION_API.md](documents/COMPANY_LOCATION_API.md)** | Lưu tọa độ địa lý, tìm kiếm công ty lân cận bằng PostGIS |
| 7 | **[EMPLOYMENT_API.md](documents/EMPLOYMENT_API.md)** | Quản lý ca làm việc (Shifts), nhân viên, tính bảng lương |
| 8 | **[COMMUNITY_POST_API.md](documents/COMMUNITY_POST_API.md)** | Đăng bài viết cộng đồng, đính kèm ảnh, quản lý bài viết |
| 9 | **[FEED_API.md](documents/FEED_API.md)** | Bảng tin hỗn hợp (Bài viết & Job) phân trang vô tận |
| 10 | **[INTERACTION_API.md](documents/INTERACTION_API.md)** | Thích bài viết, bình luận, báo cáo vi phạm |
| 11 | **[CANDIDATE_SEARCH_API.md](documents/CANDIDATE_SEARCH_API.md)** | Nhà tuyển dụng tìm kiếm hồ sơ ứng viên theo từ khóa/kỹ năng |
| 12 | **[PROFILE_API.md](documents/PROFILE_API.md)** | Cập nhật hồ sơ cá nhân của Ứng viên & Nhà tuyển dụng |
| 13 | **[CV_PROFILE_API.md](documents/CV_PROFILE_API.md)** | Quản lý mẫu CV trực tuyến |
| 14 | **[SAVED_JOB_API.md](documents/SAVED_JOB_API.md)** | Lưu và quản lý danh sách tin tuyển dụng yêu thích |
| 15 | **[NOTIFICATION_API.md](documents/NOTIFICATION_API.md)** | Quản lý thông báo người dùng |

---

## 👥 8. Đóng góp & Phát triển (Contributing)

1. Fork dự án về tài khoản cá nhân.
2. Tạo nhánh tính năng mới (`git checkout -b feature/AmazingFeature`).
3. Commit các thay đổi (`git commit -m 'feat: Add some AmazingFeature'`).
4. Push lên nhánh vừa tạo (`git push origin feature/AmazingFeature`).
5. Tạo một **Pull Request** trên GitHub để được review và merge.

---

<p align="center">
  <b>UniPath API</b> — Nâng bước con đường sự nghiệp của sinh viên Việt Nam 🎓💼
</p>

# 📋 Hướng dẫn Test API UniPath trên Postman (Cập nhật Mới nhất)

Tài liệu mẫu request đầy đủ của toàn bộ **13 Phân hệ API** dự án **UniPath API** (Spring Boot), chuẩn hóa theo đúng cấu trúc cơ sở dữ liệu `schema_v2` và mối quan hệ **1-N Công ty - Nhà tuyển dụng**.

> **📌 Quy tắc Header Xác thực (Authorization):**
> Đối với tất cả các API yêu cầu xác thực, đính kèm Header:
> `Authorization`: `Bearer <token_jwt_sau_khi_login>`

---

## 🔑 1. Xác thực & Khởi tạo Tài khoản (Auth & Registration)

### 1.1 Đăng ký tài khoản Nhà tuyển dụng mới (Employer)
- **Phương thức:** `POST`
- **URL API:** `http://localhost:8080/api/auth/register`
- **Nội dung Body (raw JSON):**
```json
{
  "email": "hr_manager@company.com",
  "password": "123456",
  "role": "EMPLOYER"
}
```

---

### 1.2 Đăng ký tài khoản Ứng viên mới (Candidate)
- **Phương thức:** `POST`
- **URL API:** `http://localhost:8080/api/auth/register`
- **Nội dung Body (raw JSON):**
```json
{
  "email": "candidate1@example.com",
  "password": "123456",
  "role": "CANDIDATE"
}
```

---

### 1.3 Đăng nhập (Lấy Token JWT)
- **Phương thức:** `POST`
- **URL API:** `http://localhost:8080/api/auth/login`
- **Nội dung Body (raw JSON):**
```json
{
  "email": "hr_manager@company.com",
  "password": "123456"
}
```

---

### 1.4 Đổi mật khẩu
- **Phương thức:** `POST`
- **URL API:** `http://localhost:8080/api/auth/change-password`
- **Nội dung Body (raw JSON):**
```json
{
  "oldPassword": "123456",
  "newPassword": "newPassword123!"
}
```

---

## 🏢 2. Phân hệ Công ty & Quản lý Đa Thành viên (Company Management)

### 2.1 Đề xuất tạo Công ty mới (Employer)
- **Phương thức:** `POST`
- **URL API:** `http://localhost:8080/api/companies`
- **Nội dung Body (raw JSON):**
```json
{
  "companyName": "Công ty TNHH Giải Pháp Công Nghệ UniPath",
  "companyScale": "SME",
  "description": "Giải pháp tuyển dụng kết nối sinh viên và doanh nghiệp",
  "website": "https://unipath.vn",
  "taxCode": "0109999999",
  "phoneNumber": "0987654321",
  "businessLicenseUrl": "https://example.com/license.pdf"
}
```
*Lưu ý: Công ty mới tạo có `status = 'PENDING'`. Hệ thống tự động thêm người tạo vào `company_members` với `member_role = 'COMPANY_ADMIN'`.*

---

### 2.2 Tìm kiếm Công ty theo tên (Chỉ tìm Công ty đã APPROVED)
- **Phương thức:** `GET`
- **URL API:** `http://localhost:8080/api/companies/search?keyword=UniPath`
- **Nội dung Body:** *(None)*

---

### 2.3 Xem chi tiết Công ty
- **Phương thức:** `GET`
- **URL API:** `http://localhost:8080/api/companies/1`
- **Nội dung Body:** *(None)*

---

### 2.4 Cập nhật thông tin Công ty (Yêu cầu vai trò COMPANY_ADMIN)
- **Phương thức:** `PUT`
- **URL API:** `http://localhost:8080/api/companies/1`
- **Nội dung Body (raw JSON):**
```json
{
  "companyName": "Công ty TNHH Công Nghệ UniPath (Cập nhật)",
  "companyScale": "ENTERPRISE",
  "description": "Nền tảng tuyển dụng sinh viên hàng đầu Việt Nam",
  "website": "https://unipath.vn",
  "taxCode": "0109999999",
  "phoneNumber": "0987654321",
  "businessLicenseUrl": "https://example.com/license_v2.pdf"
}
```

---

### 2.5 Danh sách các Công ty của tôi (Cho xem memberRole)
- **Phương thức:** `GET`
- **URL API:** `http://localhost:8080/api/users/me/companies`
- **Nội dung Body:** *(None)*

---

### 2.6 Gửi yêu cầu gia nhập Công ty đã APPROVED (Employer mới)
- **Phương thức:** `POST`
- **URL API:** `http://localhost:8080/api/companies/1/join-requests`
- **Nội dung Body (raw JSON):**
```json
{
  "message": "Tôi là nhân viên HR mới muốn tham gia công ty để đăng tin tuyển dụng"
}
```

---

### 2.7 Xem danh sách yêu cầu xin gia nhập đang chờ duyệt
- **Phương thức:** `GET`
- **URL API:** `http://localhost:8080/api/companies/1/join-requests?status=PENDING`
- **Nội dung Body:** *(None)*

---

### 2.8 Duyệt yêu cầu gia nhập (Company Admin / System Admin)
- **Phương thức:** `PATCH`
- **URL API:** `http://localhost:8080/api/join-requests/1/approve`
- **Nội dung Body:** *(None)*

---

### 2.9 Từ chối yêu cầu gia nhập
- **Phương thức:** `PATCH`
- **URL API:** `http://localhost:8080/api/join-requests/1/reject`
- **Nội dung Body:** *(None)*

---

### 2.10 System Admin Xem danh sách Công ty chờ duyệt
- **Phương thức:** `GET`
- **URL API:** `http://localhost:8080/api/admin/companies?status=PENDING`
- **Nội dung Body:** *(None)*

---

### 2.11 System Admin Chấp thuận Công ty mới
- **Phương thức:** `PATCH`
- **URL API:** `http://localhost:8080/api/admin/companies/1/approve`
- **Nội dung Body:** *(None)*

---

### 2.12 System Admin Từ chối Công ty mới
- **Phương thức:** `PATCH`
- **URL API:** `http://localhost:8080/api/admin/companies/1/reject`
- **Nội dung Body:** *(None)*

---

### 2.13 Thêm vị trí địa lý cho Công ty (Company Admin)
- **Phương thức:** `POST`
- **URL API:** `http://localhost:8080/api/companies/add`
- **Nội dung Body (raw JSON):**
```json
{
  "companyId": 1,
  "address": "Số 1 Đại Cồ Việt, Hai Bà Trưng, Hà Nội",
  "lat": 21.0069,
  "lon": 105.8432
}
```

---

### 2.14 Tìm công ty lân cận theo bán kính
- **Phương thức:** `GET`
- **URL API:** `http://localhost:8080/api/companies/nearby?lat=21.0069&lon=105.8432&radius=5000`
- **Nội dung Body:** *(None)*

---

## 👔 3. Quản lý Nhân sự SME (Tạo Hợp đồng & Ca làm)

### 3.1 Tạo Hợp đồng Nhân viên nội bộ mới (Create Employment)
- **Phương thức:** `POST`
- **URL API:** `http://localhost:8080/api/employer/employees`
- **Nội dung Body (raw JSON):**
```json
{
  "candidateId": 4,
  "baseSalaryPerHour": 35000,
  "startDate": "2026-08-01"
}
```

---

### 3.2 Danh sách Nhân viên nội bộ
- **Phương thức:** `GET`
- **URL API:** `http://localhost:8080/api/employer/employees`
- **Nội dung Body:** *(None)*

---

### 3.3 Ca làm việc hôm nay (SME)
- **Phương thức:** `GET`
- **URL API:** `http://localhost:8080/api/employer/shifts/today`
- **Nội dung Body:** *(None)*

---

### 3.4 Tạo ca làm mới cho nhân viên
- **Phương thức:** `POST`
- **URL API:** `http://localhost:8080/api/employer/shifts`
- **Nội dung Body (raw JSON):**
```json
{
  "employmentId": 1,
  "shiftDate": "2026-08-01",
  "startTime": "08:00:00",
  "endTime": "12:00:00",
  "locationId": 1
}
```

---

### 3.5 Bảng lương nội bộ
- **Phương thức:** `GET`
- **URL API:** `http://localhost:8080/api/employer/payroll?month=8&year=2026`
- **Nội dung Body:** *(None)*

---

## 👤 4. Hồ sơ cá nhân (Profiles)

### 4.1 Xem hồ sơ Ứng viên (Candidate)
- **Phương thức:** `GET`
- **URL API:** `http://localhost:8080/api/profiles/candidate/4`
- **Nội dung Body:** *(None)*

---

### 4.2 Cập nhật hồ sơ Ứng viên
- **Phương thức:** `PUT`
- **URL API:** `http://localhost:8080/api/profiles/candidate/4`
- **Nội dung Body (raw JSON):**
```json
{
  "fullName": "Nguyễn Văn A",
  "phoneNumber": "0987654321",
  "experienceYears": 2,
  "skills": "Java, Spring Boot, PostgreSQL",
  "universityName": "Đại học Bách Khoa",
  "major": "Công nghệ thông tin"
}
```

---

### 4.3 Xem hồ sơ Nhà tuyển dụng (Employer)
- **Phương thức:** `GET`
- **URL API:** `http://localhost:8080/api/profiles/employer/4`
- **Nội dung Body:** *(None)*

---

### 4.4 Cập nhật hồ sơ Nhà tuyển dụng
- **Phương thức:** `PUT`
- **URL API:** `http://localhost:8080/api/profiles/employer/4`
- **Nội dung Body (raw JSON):**
```json
{
  "fullName": "Trần Thị B",
  "phone": "0912345678",
  "position": "Trưởng phòng Nhân sự",
  "bio": "Hơn 5 năm kinh nghiệm tuyển dụng IT."
}
```

---

## 💼 5. Tin tuyển dụng (Jobs)

### 5.1 Tạo tin tuyển dụng mới (Employer thuộc công ty)
- **Phương thức:** `POST`
- **URL API:** `http://localhost:8080/api/jobs`
- **Nội dung Body (raw JSON):**
```json
{
  "title": "Lập trình viên Java Spring Boot Junior",
  "jobType": "FULL_TIME",
  "salaryRange": "12 - 18 triệu",
  "description": "Phát triển RESTful API Backend cho hệ thống UniPath",
  "requirements": "{\"skills\": [\"Java\", \"Spring Boot\"]}",
  "categoryId": 1,
  "locationId": 1,
  "skillIds": [1, 2]
}
```

---

### 5.2 Xem chi tiết tin tuyển dụng
- **Phương thức:** `GET`
- **URL API:** `http://localhost:8080/api/jobs/1`
- **Nội dung Body:** *(None)*

---

### 5.3 Chỉnh sửa tin tuyển dụng
- **Phương thức:** `PUT`
- **URL API:** `http://localhost:8080/api/jobs/1`
- **Nội dung Body (raw JSON):**
```json
{
  "title": "Senior Java Developer (Spring Boot / Microservices)",
  "jobType": "FULL_TIME",
  "salaryRange": "20 - 30 triệu",
  "description": "Cập nhật yêu cầu lập trình viên cao cấp...",
  "requirements": "{\"experience\": \"3+ năm kinh nghiệm\"}",
  "categoryId": 1,
  "locationId": 1,
  "skillIds": [1, 2, 3]
}
```

---

### 5.4 Đóng tin tuyển dụng
- **Phương thức:** `PATCH`
- **URL API:** `http://localhost:8080/api/jobs/1/close`
- **Nội dung Body:** *(None)*

---

### 5.5 Xoá tin tuyển dụng (Chưa có ứng viên)
- **Phương thức:** `DELETE`
- **URL API:** `http://localhost:8080/api/jobs/1`
- **Nội dung Body:** *(None)*

---

### 5.6 Feed tin tuyển dụng (Cursor-based)
- **Phương thức:** `GET`
- **URL API:** `http://localhost:8080/api/jobs/feed?keyword=Java`
- **Nội dung Body:** *(None)*

---

## 📝 6. Bài viết cộng đồng & Feed (Community Posts & Feed)

### 6.1 Đăng bài viết mới
- **Phương thức:** `POST`
- **URL API:** `http://localhost:8080/api/posts`
- **Nội dung Body (raw JSON):**
```json
{
  "title": "Kinh nghiệm phỏng vấn Java Developer 2026",
  "content": "Chia sẻ chi tiết các câu hỏi hay gặp về Spring Boot Core, JPA Hibernate và PostgreSQL..."
}
```

---

### 6.2 Xem chi tiết bài viết
- **Phương thức:** `GET`
- **URL API:** `http://localhost:8080/api/posts/1`
- **Nội dung Body:** *(None)*

---

### 6.3 Sửa bài viết
- **Phương thức:** `PUT`
- **URL API:** `http://localhost:8080/api/posts/1`
- **Nội dung Body (raw JSON):**
```json
{
  "title": "[Cập nhật] Kinh nghiệm phỏng vấn Java Developer 2026",
  "content": "Cập nhật thêm câu hỏi về Docker & Microservices..."
}
```

---

### 6.4 Xoá bài viết
- **Phương thức:** `DELETE`
- **URL API:** `http://localhost:8080/api/posts/1`
- **Nội dung Body:** *(None)*

---

### 6.5 Feed gộp cả Jobs & Posts
- **Phương thức:** `GET`
- **URL API:** `http://localhost:8080/api/feed`
- **Nội dung Body:** *(None)*

---

## ❤️ 7. Tương tác (Like, Comment, Share)

### 7.1 Thích tin tuyển dụng
- **Phương thức:** `POST`
- **URL API:** `http://localhost:8080/api/jobs/1/like`
- **Nội dung Body:** *(None)*

---

### 7.2 Bỏ thích tin tuyển dụng
- **Phương thức:** `DELETE`
- **URL API:** `http://localhost:8080/api/jobs/1/like`
- **Nội dung Body:** *(None)*

---

### 7.3 Thích bài viết cộng đồng
- **Phương thức:** `POST`
- **URL API:** `http://localhost:8080/api/posts/1/like`
- **Nội dung Body:** *(None)*

---

### 7.4 Bình luận tin tuyển dụng (Trả lời reply: truyền parentCommentId)
- **Phương thức:** `POST`
- **URL API:** `http://localhost:8080/api/jobs/1/comments`
- **Nội dung Body (raw JSON):**
```json
{
  "content": "Vị trí này có yêu cầu bằng đại học không ạ?",
  "parentCommentId": null
}
```

---

### 7.5 Xem bình luận tin tuyển dụng
- **Phương thức:** `GET`
- **URL API:** `http://localhost:8080/api/jobs/1/comments`
- **Nội dung Body:** *(None)*

---

### 7.6 Xoá bình luận
- **Phương thức:** `DELETE`
- **URL API:** `http://localhost:8080/api/comments/1`
- **Nội dung Body:** *(None)*

---

### 7.7 Chia sẻ tin tuyển dụng
- **Phương thức:** `POST`
- **URL API:** `http://localhost:8080/api/jobs/1/share`
- **Nội dung Body:** *(None)*

---

## 📄 8. Ứng tuyển & File CV (Applications)

### 8.1 Kiểm tra đã ứng tuyển chưa
- **Phương thức:** `GET`
- **URL API:** `http://localhost:8080/api/jobs/1/check-applied`
- **Nội dung Body:** *(None)*

---

### 8.2 Nộp hồ sơ ứng tuyển (Multipart Upload)
- **Phương thức:** `POST`
- **URL API:** `http://localhost:8080/api/applications`
- **Nội dung Body (`form-data`):**
  - Key `data` (text, `application/json`): `{"jobId": 1, "coverLetter": "Tôi rất mong muốn ứng tuyển..."}`
  - Key `cvFile` (File): Chọn file `.pdf` hoặc `.docx` (<= 5MB)

---

### 8.3 Lịch sử ứng tuyển của Ứng viên
- **Phương thức:** `GET`
- **URL API:** `http://localhost:8080/api/applications/me`
- **Nội dung Body:** *(None)*

---

### 8.4 Danh sách ứng viên theo Job (Employer)
- **Phương thức:** `GET`
- **URL API:** `http://localhost:8080/api/jobs/1/applications`
- **Nội dung Body:** *(None)*

---

### 8.5 Cập nhật trạng thái ứng tuyển (Employer)
- **Phương thức:** `PATCH`
- **URL API:** `http://localhost:8080/api/applications/1/status`
- **Nội dung Body (raw JSON):**
```json
{
  "status": "ACCEPTED",
  "note": "Trân trọng mời ứng viên phỏng vấn vào thứ 2 tuần tới lúc 9:00"
}
```

---

### 8.6 Tải file CV
- **Phương thức:** `GET`
- **URL API:** `http://localhost:8080/api/applications/1/cv/download`
- **Nội dung Body:** *(None)*

---

## ⭐ 9. Lưu tin tuyển dụng (Saved Jobs)

### 9.1 Lưu tin tuyển dụng
- **Phương thức:** `POST`
- **URL API:** `http://localhost:8080/api/jobs/1/save`
- **Nội dung Body:** *(None)*

---

### 9.2 Bỏ lưu tin tuyển dụng
- **Phương thức:** `DELETE`
- **URL API:** `http://localhost:8080/api/jobs/1/save`
- **Nội dung Body:** *(None)*

---

### 9.3 Xem danh sách tin tuyển dụng đã lưu
- **Phương thức:** `GET`
- **URL API:** `http://localhost:8080/api/saved-jobs`
- **Nội dung Body:** *(None)*

---

## 🤖 10. Mẫu CV AI (CV Profiles)

### 10.1 Tạo mẫu CV AI mới
- **Phương thức:** `POST`
- **URL API:** `http://localhost:8080/api/cv-profiles`
- **Nội dung Body (raw JSON):**
```json
{
  "title": "CV Lập trình viên Java Web 2026",
  "contentJson": "{\"summary\": \"2 năm kinh nghiệm phát triển Java Spring Boot\"}"
}
```

---

### 10.2 Danh sách CV của tôi
- **Phương thức:** `GET`
- **URL API:** `http://localhost:8080/api/cv-profiles`
- **Nội dung Body:** *(None)*

---

### 10.3 Đặt làm CV mặc định
- **Phương thức:** `PATCH`
- **URL API:** `http://localhost:8080/api/cv-profiles/1/set-primary`
- **Nội dung Body:** *(None)*

---

## 🔍 11. Tìm kiếm ứng viên (Candidate Search)

### 11.1 Tìm kiếm hồ sơ ứng viên
- **Phương thức:** `GET`
- **URL API:** `http://localhost:8080/api/candidates/search?skill=Java&university=Bách+Khoa`
- **Nội dung Body:** *(None)*

---

## 🛡️ 12. Quản trị viên (System Admin)

### 12.1 Xem danh sách người dùng
- **Phương thức:** `GET`
- **URL API:** `http://localhost:8080/api/admin/users?type=CANDIDATE`
- **Nội dung Body:** *(None)*

---

### 12.2 Khóa tài khoản người dùng
- **Phương thức:** `PATCH`
- **URL API:** `http://localhost:8080/api/admin/users/4/ban`
- **Nội dung Body:** *(None)*

---

### 12.3 Mở khóa tài khoản người dùng
- **Phương thức:** `PATCH`
- **URL API:** `http://localhost:8080/api/admin/users/4/unban`
- **Nội dung Body:** *(None)*

---

### 12.4 Báo cáo tin tuyển dụng vi phạm (Candidate)
- **Phương thức:** `POST`
- **URL API:** `http://localhost:8080/api/jobs/1/report`
- **Nội dung Body (raw JSON):**
```json
{
  "reason": "Tin tuyển dụng yêu cầu đặt cọc phí bất hợp pháp"
}
```

---

### 12.5 Admin xử lý báo cáo vi phạm
- **Phương thức:** `PATCH`
- **URL API:** `http://localhost:8080/api/admin/reports/1/resolve`
- **Nội dung Body (raw JSON):**
```json
{
  "action": "BAN_ACCOUNT"
}
```

---

### 12.6 Báo cáo thống kê Dashboard Admin
- **Phương thức:** `GET`
- **URL API:** `http://localhost:8080/api/admin/stats`
- **Nội dung Body:** *(None)*

---

## 🔔 13. Thông báo (Notifications)

### 13.1 Danh sách thông báo của tôi
- **Phương thức:** `GET`
- **URL API:** `http://localhost:8080/api/notifications?unreadOnly=false`
- **Nội dung Body:** *(None)*

---

### 13.2 Đánh dấu 1 thông báo là đã đọc
- **Phương thức:** `PATCH`
- **URL API:** `http://localhost:8080/api/notifications/1/read`
- **Nội dung Body:** *(None)*

---

### 13.3 Đánh dấu tất cả thông báo là đã đọc
- **Phương thức:** `PATCH`
- **URL API:** `http://localhost:8080/api/notifications/read-all`
- **Nội dung Body:** *(None)*

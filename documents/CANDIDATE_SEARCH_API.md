# Tài liệu API Tìm kiếm Ứng viên (Candidate Search API)

Tài liệu chi tiết API tìm kiếm hồ sơ ứng viên dành cho Nhà tuyển dụng trong dự án **UniPath API**.

---

## 📌 Thông tin chung
- **Base URL:** `/api/candidates`
- **Xác thực:** Yêu cầu Header `Authorization: Bearer <token>`.

---

## 📑 Danh sách API

### 1. Tìm kiếm danh sách ứng viên (`GET /api/candidates/search`)
- **HTTP Method:** `GET`
- **URL Path:** `/api/candidates/search`
- **Mô tả:** Cho phép Nhà tuyển dụng tìm kiếm hồ sơ ứng viên phù hợp dựa trên các tiêu chí kỹ năng (`skill`), chuyên ngành (`major`), trường đại học (`university`) hoặc từ khoá chung (`keyword`).

#### Query Parameters
| Parameter | Kiểu dữ liệu | Bắt buộc | Mô tả |
| :--- | :--- | :---: | :--- |
| `skill` | `String` | Không | Lọc theo từ khoá kỹ năng (ví dụ: "Java", "React") |
| `major` | `String` | Không | Lọc theo chuyên ngành học (ví dụ: "Công nghệ thông tin") |
| `university` | `String` | Không | Lọc theo tên trường đại học (ví dụ: "Bách Khoa") |
| `keyword` | `String` | Không | Từ khoá tổng hợp tìm trong tên, trường, ngành và kỹ năng |

#### Ví dụ Request URL
```http
GET /api/candidates/search?skill=Java&university=Bách+Khoa
```

#### Response Structure (List<CandidateSearchResponse>)
```json
[
  {
    "userId": 1,
    "fullName": "Nguyễn Văn A",
    "universityName": "Đại học Bách Khoa",
    "major": "Công nghệ thông tin",
    "experienceYears": 2,
    "phoneNumber": "0987654321",
    "skills": "Java, Spring Boot, PostgreSQL, Docker"
  }
]
```

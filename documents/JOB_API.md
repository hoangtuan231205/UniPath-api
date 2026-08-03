# Tài liệu API Quản lý Tin Tuyển dụng (Job API)

Tài liệu chi tiết các API tạo, cập nhật, đóng, xoá, xem chi tiết và danh sách tin tuyển dụng theo phân trang cursor cho dự án **UniPath API**.

---

## 📌 Thông tin chung
- **Base URL:** `/api/jobs`
- **Xác thực:** Yêu cầu Header `Authorization: Bearer <token>` đối với các API làm thay đổi dữ liệu (POST, PUT, PATCH, DELETE).

---

## 📑 Danh sách API

### 1. Đăng tin tuyển dụng mới (`POST /api/jobs`)
- **HTTP Method:** `POST`
- **URL Path:** `/api/jobs`
- **Mô tả:** Nhà tuyển dụng tạo tin tuyển dụng mới. Mặc định `isActive = false` (chờ Admin duyệt).

#### Request Body (JobRequest)
| Trường | Kiểu dữ liệu | Bắt buộc | Mô tả |
| :--- | :--- | :---: | :--- |
| `title` | `String` | Có | Tiêu đề tin tuyển dụng |
| `jobType` | `String` | Không | Loại hình làm việc (FULL_TIME, PART_TIME, INTERN,...) |
| `salaryRange` | `String` | Không | Mức lương (ví dụ: "10-15 triệu", "Thỏa thuận") |
| `description` | `String` | Không | Mô tả công việc |
| `requirements` | `String` | Không | Yêu cầu công việc (định dạng JSON string) |
| `categoryId` | `Integer` | Không | ID danh mục ngành nghề |
| `locationId` | `Integer` | Không | ID vị trí địa lý của công ty |
| `skillIds` | `List<Integer>` | Không | Danh sách ID kỹ năng yêu cầu |

#### Ví dụ Request
```json
{
  "title": "Lập trình viên Java Spring Boot Junior",
  "jobType": "FULL_TIME",
  "salaryRange": "12 - 18 triệu",
  "description": "Phát triển các RESTful API backend cho dự án UniPath",
  "requirements": "{\"experience\": \"1 năm\", \"skills\": [\"Java\", \"Spring Boot\", \"PostgreSQL\"]}",
  "categoryId": 1,
  "locationId": 10,
  "skillIds": [1, 2, 5]
}
```

#### Response Structure (JobResponse)
| Trường | Kiểu dữ liệu | Mô tả |
| :--- | :--- | :--- |
| `id` | `Integer` | ID tin tuyển dụng vừa tạo |
| `title` | `String` | Tiêu đề tin tuyển dụng |
| `jobType` | `String` | Loại công việc |
| `salaryRange` | `String` | Mức lương |
| `description` | `String` | Mô tả |
| `requirements` | `String` | Yêu cầu |
| `categoryId` | `Integer` | ID danh mục |
| `categoryName` | `String` | Tên danh mục |
| `locationId` | `Integer` | ID vị trí |
| `locationAddress` | `String` | Địa chỉ công ty |
| `companyId` | `Integer` | ID công ty |
| `companyName` | `String` | Tên công ty |
| `companyScale` | `String` | Quy mô công ty (ENTERPRISE / SME) |
| `skills` | `List<String>` | Danh sách tên kỹ năng |
| `likesCount` | `long` | Số lượt thắc/thích |
| `commentsCount` | `long` | Số bình luận |
| `sharesCount` | `long` | Số lượt chia sẻ |
| `applicationsCount` | `long` | Số lượt ứng tuyển |
| `isActive` | `Boolean` | Trạng thái hiển thị tin (`false` khi vừa tạo) |
| `postedAt` | `LocalDateTime` | Thời gian đăng tin |
| `type` | `String` | Luôn trả về `"JOB"` |

---

### 2. Xem chi tiết tin tuyển dụng (`GET /api/jobs/{id}`)
- **HTTP Method:** `GET`
- **URL Path:** `/api/jobs/{id}`
- **Mô tả:** Lấy thông tin chi tiết của 1 tin tuyển dụng kèm thông tin công ty, kỹ năng và các số liệu lượt tương tác/ứng tuyển.

#### Response Status
- **200 OK:** Trả về đối tượng `JobResponse`.
- **404 Not Found:** Không tìm thấy tin tuyển dụng với ID tương ứng.

---

### 3. Chỉnh sửa tin tuyển dụng (`PUT /api/jobs/{id}`)
- **HTTP Method:** `PUT`
- **URL Path:** `/api/jobs/{id}`
- **Mô tả:** Nhà tuyển dụng chỉnh sửa thông tin tin tuyển dụng. Kiểm tra quyền sở hữu công ty qua token.

#### Request Body
Tương tự `JobRequest` của API tạo mới.

---

### 4. Đóng tin tuyển dụng (`PATCH /api/jobs/{id}/close`)
- **HTTP Method:** `PATCH`
- **URL Path:** `/api/jobs/{id}/close`
- **Mô tả:** Đổi trạng thái tin tuyển dụng sang `isActive = false` mà không xoá dữ liệu khỏi hệ thống.

---

### 5. Xoá tin tuyển dụng (`DELETE /api/jobs/{id}`)
- **HTTP Method:** `DELETE`
- **URL Path:** `/api/jobs/{id}`
- **Mô tả:** Xoá tin tuyển dụng.
- **Ràng buộc:** Chỉ cho phép xoá khi `applicationsCount == 0`. Nếu đã có người ứng tuyển sẽ trả về lỗi Bad Request.

#### Response Lỗi (400 Bad Request)
```text
Không thể xoá tin đã có người ứng tuyển
```

---

### 6. Danh sách tin tuyển dụng (Cursor-based Feed) (`GET /api/jobs/feed`)
- **HTTP Method:** `GET`
- **URL Path:** `/api/jobs/feed`
- **Mô tả:** Lấy danh sách tin tuyển dụng đang mở (`isActive = true`) theo phân trang cursor (`id < cursor ORDER BY posted_at DESC LIMIT 20`).

#### Query Parameters
| Parameter | Kiểu dữ liệu | Bắt buộc | Mô tả |
| :--- | :--- | :---: | :--- |
| `cursor` | `Integer` | Không | ID của tin tuyển dụng cuối cùng ở trang trước |
| `keyword` | `String` | Không | Từ khoá tìm kiếm theo tiêu đề hoặc mô tả |
| `categoryId` | `Integer` | Không | Lọc theo ID danh mục |
| `locationId` | `Integer` | Không | Lọc theo ID vị trí địa lý |
| `jobType` | `String` | Không | Lọc theo loại hình công việc |

#### Ví dụ Response (200 OK)
```json
[
  {
    "id": 15,
    "title": "Lập trình viên Java Spring Boot Junior",
    "jobType": "FULL_TIME",
    "salaryRange": "12 - 18 triệu",
    "companyName": "Công ty TNHH Công Nghệ UniPath",
    "likesCount": 5,
    "commentsCount": 2,
    "sharesCount": 1,
    "applicationsCount": 3,
    "isActive": true,
    "postedAt": "2026-07-28T10:00:00",
    "type": "JOB"
  }
]
```

# Tài liệu API Bài viết Cộng đồng & Merged Feed (Community Post & Feed API)

Tài liệu chi tiết các API bài đăng cộng đồng và API Feed tổng hợp gộp cả Jobs & Posts cho dự án **UniPath API**.

---

## 📌 Thông tin chung
- **Base URL:** `/api/posts` và `/api/feed`
- **Xác thực:** Yêu cầu Header `Authorization: Bearer <token>` cho các thao tác đăng/sửa/xoá bài viết.

---

## 📑 Danh sách API

### 1. Đăng bài viết mới (`POST /api/posts`)
- **HTTP Method:** `POST`
- **URL Path:** `/api/posts`
- **Mô tả:** Bất kỳ người dùng đã đăng nhập nào (Ứng viên / Nhà tuyển dụng) cũng có thể tạo bài viết cộng đồng.

#### Request Body (CommunityPostRequest)
| Trường | Kiểu dữ liệu | Bắt buộc | Mô tả |
| :--- | :--- | :---: | :--- |
| `title` | `String` | Có | Tiêu đề bài viết |
| `content` | `String` | Có | Nội dung bài viết |

#### Ví dụ Request
```json
{
  "title": "Chia sẻ kinh nghiệm phỏng vấn vị trí Java Developer",
  "content": "Hôm nay mình xin chia sẻ một số câu hỏi hay gặp khi phỏng vấn Spring Boot và PostgreSQL..."
}
```

---

### 2. Xem chi tiết bài viết (`GET /api/posts/{id}`)
- **HTTP Method:** `GET`
- **URL Path:** `/api/posts/{id}`
- **Mô tả:** Lấy thông tin bài viết kèm thông tin tác giả, số lượt like và số bình luận.

---

### 3. Chỉnh sửa bài viết (`PUT /api/posts/{id}`)
- **HTTP Method:** `PUT`
- **URL Path:** `/api/posts/{id}`
- **Mô tả:** Chỉnh sửa bài viết (chỉ tác giả bài viết mới có quyền chỉnh sửa).

---

### 4. Xoá bài viết (`DELETE /api/posts/{id}`)
- **HTTP Method:** `DELETE`
- **URL Path:** `/api/posts/{id}`
- **Mô tả:** Xoá bài viết (chỉ tác giả bài viết mới có quyền xoá).

---

### 5. Feed Tổng hợp gộp (`GET /api/feed`)
- **HTTP Method:** `GET`
- **URL Path:** `/api/feed`
- **Mô tả:** Merge cả tin tuyển dụng (`jobs`) và bài viết cộng đồng (`community_posts`) theo thứ tự thời gian mới nhất, trả về thuộc tính `type: "JOB" | "POST"` để Frontend phân biệt thẻ hiển thị.

#### Query Parameters
| Parameter | Kiểu dữ liệu | Bắt buộc | Mô tả |
| :--- | :--- | :---: | :--- |
| `cursor` | `Integer` | Không | ID mốc phân trang cursor |

#### Ví dụ Response (200 OK)
```json
[
  {
    "type": "POST",
    "id": 8,
    "title": "Chia sẻ kinh nghiệm phỏng vấn vị trí Java Developer",
    "content": "Hôm nay mình xin chia sẻ một số câu hỏi hay gặp...",
    "authorOrCompany": "Nguyễn Văn A",
    "timestamp": "2026-07-28T10:30:00",
    "postDetails": {
      "id": 8,
      "authorId": 1,
      "authorName": "Nguyễn Văn A",
      "title": "Chia sẻ kinh nghiệm phỏng vấn vị trí Java Developer",
      "content": "Hôm nay mình xin chia sẻ...",
      "createdAt": "2026-07-28T10:30:00",
      "likesCount": 12,
      "commentsCount": 4,
      "type": "POST"
    }
  },
  {
    "type": "JOB",
    "id": 15,
    "title": "Lập trình viên Java Spring Boot Junior",
    "content": "Phát triển các RESTful API backend...",
    "authorOrCompany": "Công ty TNHH Công Nghệ UniPath",
    "timestamp": "2026-07-28T10:00:00",
    "jobDetails": {
      "id": 15,
      "title": "Lập trình viên Java Spring Boot Junior",
      "jobType": "FULL_TIME",
      "salaryRange": "12 - 18 triệu",
      "companyName": "Công ty TNHH Công Nghệ UniPath",
      "type": "JOB"
    }
  }
]
```

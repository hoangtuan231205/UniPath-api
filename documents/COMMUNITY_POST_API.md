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

#### Response Structure (CommunityPostResponse)
| Trường | Kiểu dữ liệu | Mô tả |
| :--- | :--- | :--- |
| `id` | `Integer` | ID bài viết |
| `authorId` | `Integer` | User ID của tác giả |
| `authorName` | `String` | Tên tác giả bài viết |
| `authorAvatarUrl` | `String` | Đường dẫn ảnh đại diện tác giả |
| `title` | `String` | Tiêu đề bài viết |
| `content` | `String` | Nội dung bài viết |
| `createdAt` | `LocalDateTime` | Thời gian tạo bài viết |
| `likesCount` | `long` | Số lượt thích/thả tim |
| `commentsCount` | `long` | Số bình luận |
| `isLiked` | `Boolean` | Trạng thái người dùng hiện tại đã like hay chưa (`true`/`false`) |
| `type` | `String` | Luôn trả về `"POST"` |

---

### 2. Xem danh sách bài viết cá nhân đã đăng (`GET /api/posts/me`)
- **HTTP Method:** `GET`
- **URL Path:** `/api/posts/me`
- **Mô tả:** Lấy danh sách tất cả các bài viết do chính người dùng hiện tại (dựa trên JWT Token) đã tạo.
- **Xác thực:** **Bắt buộc** Header `Authorization: Bearer <token>`
- **Query Parameters:**
  - `cursor` (`Integer`, tùy chọn): Phân trang theo con trỏ cursor.

---

### 3. Xem danh sách bài viết của một người dùng khác (`GET /api/posts/user/{userId}`)
- **HTTP Method:** `GET`
- **URL Path:** `/api/posts/user/{userId}`
- **Mô tả:** Lấy danh sách các bài viết do người dùng có ID `userId` tạo.
- **Header:** `Authorization: Bearer <token>` (tùy chọn, để xác định cờ `isLiked`).
- **Query Parameters:**
  - `cursor` (`Integer`, tùy chọn): Phân trang theo con trỏ cursor.

---

### 4. Xem chi tiết bài viết (`GET /api/posts/{id}`)
- **HTTP Method:** `GET`
- **URL Path:** `/api/posts/{id}`
- **Mô tả:** Lấy thông tin chi tiết bài viết kèm thông tin tác giả, số lượt like và số bình luận.

---

### 5. Chỉnh sửa bài viết (`PUT /api/posts/{id}`)
- **HTTP Method:** `PUT`
- **URL Path:** `/api/posts/{id}`
- **Mô tả:** Chỉnh sửa bài viết (chỉ tác giả bài viết mới có quyền chỉnh sửa).

---

### 6. Xoá bài viết (`DELETE /api/posts/{id}`)
- **HTTP Method:** `DELETE`
- **URL Path:** `/api/posts/{id}`
- **Mô tả:** Xoá bài viết (chỉ tác giả bài viết mới có quyền xoá).

---

### 7. Feed Tổng hợp gộp (`GET /api/feed`)
- **HTTP Method:** `GET`
- **URL Path:** `/api/feed`
- **Mô tả:** Merge cả tin tuyển dụng (`jobs`) và bài viết cộng đồng (`community_posts`) theo thứ tự thời gian mới nhất, trả về thuộc tính `type: "JOB" | "POST"` để Frontend phân biệt thẻ hiển thị. Trả về `avatarUrl` và `isLiked` theo token người dùng.

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
    "avatarUrl": "https://example.com/avatar1.jpg",
    "isLiked": true,
    "timestamp": "2026-07-28T10:30:00",
    "postDetails": {
      "id": 8,
      "authorId": 1,
      "authorName": "Nguyễn Văn A",
      "authorAvatarUrl": "https://example.com/avatar1.jpg",
      "title": "Chia sẻ kinh nghiệm phỏng vấn vị trí Java Developer",
      "content": "Hôm nay mình xin chia sẻ...",
      "createdAt": "2026-07-28T10:30:00",
      "likesCount": 12,
      "commentsCount": 4,
      "isLiked": true,
      "type": "POST"
    }
  },
  {
    "type": "JOB",
    "id": 15,
    "title": "Lập trình viên Java Spring Boot Junior",
    "content": "Phát triển các RESTful API backend...",
    "authorOrCompany": "Công ty TNHH Công Nghệ UniPath",
    "avatarUrl": "https://example.com/company_logo.png",
    "isLiked": false,
    "timestamp": "2026-07-28T10:00:00",
    "jobDetails": {
      "id": 15,
      "title": "Lập trình viên Java Spring Boot Junior",
      "jobType": "FULL_TIME",
      "salaryRange": "12 - 18 triệu",
      "companyName": "Công ty TNHH Công Nghệ UniPath",
      "companyLogoUrl": "https://example.com/company_logo.png",
      "isLiked": false,
      "type": "JOB"
    }
  }
]
```

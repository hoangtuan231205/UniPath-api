# Tài liệu API Bảng tin Cộng đồng Tổng hợp (Feed API)

Tài liệu chi tiết API Bảng tin tổng hợp (Feed) dùng để lấy dữ liệu bài viết cộng đồng và tin tuyển dụng cho ứng dụng **UniPath API**.

---

## 📌 Thông tin chung
- **Base URL:** `/api/feed`
- **CORS:** Cho phép tất cả origins (`@CrossOrigin(origins = "*")`)
- **Định dạng dữ liệu:** `application/json`

---

## 📑 Danh sách API

### 1. Lấy Bảng tin tổng hợp (`GET /api/feed`)
- **HTTP Method:** `GET`
- **URL Path:** `/api/feed`
- **Mô tả:** Lấy danh sách bảng tin trộn lẫn (Merged Feed) bao gồm các bài viết cộng đồng mới nhất và các công việc đề xuất, hỗ trợ cuộn vô tận (Infinite Scroll) theo con trỏ `cursor`.

- **Header (Thùy chọn):** `Authorization: Bearer <token>` (được dùng để tự động xác định trạng thái `isLiked` cho người dùng hiện tại. Nếu không truyền header, `isLiked = false`).

#### Query Parameters
| Parameter | Kiểu dữ liệu | Bắt buộc | Mô tả |
| :--- | :--- | :---: | :--- |
| `cursor` | `Integer` | Không | ID con trỏ của trang trước để lấy trang dữ liệu tiếp theo (Phân trang dạng Cursor-based Pagination) |

#### Response Structure (FeedItemResponse)
| Trường | Kiểu dữ liệu | Mô tả |
| :--- | :--- | :--- |
| `type` | `String` | Loại thẻ (`"JOB"` hoặc `"POST"`) |
| `id` | `Integer` | ID của Job hoặc Post |
| `title` | `String` | Tiêu đề |
| `content` | `String` | Mô tả ngắn / Nội dung |
| `authorOrCompany` | `String` | Tên tác giả hoặc tên công ty tuyển dụng |
| `avatarUrl` | `String` | Đường dẫn ảnh đại diện tác giả hoặc logo công ty |
| `isLiked` | `Boolean` | Trạng thái người dùng đã thả tim hay chưa (`true`/`false`) |
| `timestamp` | `LocalDateTime` | Thời điểm tạo / đăng bài |
| `jobDetails` | `JobResponse` | Chi tiết Job nếu `type == "JOB"` |
| `postDetails` | `CommunityPostResponse` | Chi tiết Post nếu `type == "POST"` |

#### Ví dụ Response (200 OK)
```json
[
  {
    "type": "POST",
    "id": 101,
    "title": "Kinh nghiệm phỏng vấn vị trí Java Backend Developer",
    "content": "Hôm nay mình xin chia sẻ một số câu hỏi phỏng vấn phổ biến...",
    "authorOrCompany": "Nguyễn Văn A",
    "avatarUrl": "https://example.com/avatars/user1.jpg",
    "isLiked": true,
    "timestamp": "2026-08-05T10:00:00"
  },
  {
    "type": "JOB",
    "id": 45,
    "title": "Senior Java Spring Boot Engineer",
    "content": "Tuyển dụng kỹ sư Java kinh nghiệm trên 3 năm...",
    "authorOrCompany": "Công ty Công nghệ UniPath",
    "avatarUrl": "https://example.com/logos/unipath.png",
    "isLiked": false,
    "timestamp": "2026-08-05T09:30:00"
  }
]
```

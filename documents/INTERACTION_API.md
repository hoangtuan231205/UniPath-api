# Tài liệu API Tương tác: Like, Comment, Share (Interaction API)

Tài liệu chi tiết các API tương tác bao gồm Thích (Like), Bình luận (Comment - hỗ trợ reply lồng nhau), và Chia sẻ (Share) dùng chung cho Tin tuyển dụng và Bài viết cộng đồng trong **UniPath API**.

---

## 📌 Thông tin chung
- **Xác thực:** Yêu cầu Header `Authorization: Bearer <token>` đối với các thao tác Thích, Bình luận, Xoá bình luận và Chia sẻ.

---

## 📑 Danh sách API

### 1. Thích / Bỏ thích Tin tuyển dụng (`POST/DELETE /api/jobs/{id}/like`)
- **HTTP Method:** `POST` (Thích) hoặc `DELETE` (Bỏ thích)
- **URL Path:** `/api/jobs/{id}/like`
- **Mô tả:** Đánh dấu thích hoặc bỏ thích 1 tin tuyển dụng.

---

### 2. Thích / Bỏ thích Bài viết cộng đồng (`POST/DELETE /api/posts/{id}/like`)
- **HTTP Method:** `POST` (Thích) hoặc `DELETE` (Bỏ thích)
- **URL Path:** `/api/posts/{id}/like`
- **Mô tả:** Đánh dấu thích hoặc bỏ thích 1 bài viết cộng đồng.

---

### 3. Lấy danh sách bình luận Job (`GET /api/jobs/{id}/comments`)
- **HTTP Method:** `GET`
- **URL Path:** `/api/jobs/{id}/comments?cursor=`
- **Mô tả:** Trả về danh sách bình luận của tin tuyển dụng theo phân trang cursor.

---

### 4. Bình luận vào Tin tuyển dụng (`POST /api/jobs/{id}/comments`)
- **HTTP Method:** `POST`
- **URL Path:** `/api/jobs/{id}/comments`
- **Mô tả:** Thêm bình luận mới vào tin tuyển dụng. Hỗ trợ trường `parentCommentId` để trả lời (reply) một bình luận khác.

#### Request Body (CommentRequest)
| Trường | Kiểu dữ liệu | Bắt buộc | Mô tả |
| :--- | :--- | :---: | :--- |
| `content` | `String` | Có | Nội dung bình luận |
| `parentCommentId` | `Integer` | Không | ID bình luận cha (dùng khi trả lời reply) |

#### Ví dụ Request
```json
{
  "content": "Vị trí này có yêu cầu tiếng Anh không ạ?",
  "parentCommentId": null
}
```

---

### 5. Xoá bình luận Job (`DELETE /api/comments/{id}`)
- **HTTP Method:** `DELETE`
- **URL Path:** `/api/comments/{id}`
- **Mô tả:** Xoá bình luận của tin tuyển dụng (chỉ tác giả bình luận mới có quyền xoá).

---

### 6. Lấy danh sách bình luận Bài viết (`GET /api/posts/{id}/comments`)
- **HTTP Method:** `GET`
- **URL Path:** `/api/posts/{id}/comments?cursor=`

---

### 7. Bình luận vào Bài viết (`POST /api/posts/{id}/comments`)
- **HTTP Method:** `POST`
- **URL Path:** `/api/posts/{id}/comments`

---

### 8. Xoá bình luận Bài viết (`DELETE /api/posts/comments/{id}`)
- **HTTP Method:** `DELETE`
- **URL Path:** `/api/posts/comments/{id}`

---

### 9. Chia sẻ Tin tuyển dụng (`POST /api/jobs/{id}/share`)
- **HTTP Method:** `POST`
- **URL Path:** `/api/jobs/{id}/share`
- **Mô tả:** Ghi nhận lượt chia sẻ tin tuyển dụng của người dùng.

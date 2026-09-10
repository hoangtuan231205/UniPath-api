# Tài liệu API Người dùng & Công ty liên kết (User API)

Tài liệu chi tiết các API quản lý tài khoản người dùng và thông tin sở hữu/liên kết công ty trong dự án **UniPath API**.

---

## 📌 Thông tin chung
- **Base URL:** `/api/users`
- **CORS:** Cho phép tất cả origins (`@CrossOrigin(origins = "*")`)
- **Xác thực:** Yêu cầu Header `Authorization: Bearer <token>`.
- **Định dạng dữ liệu:** `application/json`

---

## 📑 Danh sách API

### 1. Lấy danh sách Công ty của tôi (`GET /api/users/me/companies`)
- **HTTP Method:** `GET`
- **URL Path:** `/api/users/me/companies`
- **Mô tả:** Trả về danh sách các công ty mà người dùng hiện tại (dựa trên JWT Token) sở hữu, quản lý hoặc đang làm việc.

#### Request Headers
| Header | Kiểu dữ liệu | Bắt buộc | Mô tả |
| :--- | :--- | :---: | :--- |
| `Authorization` | `String` | Có | `Bearer <JWT_TOKEN>` |

#### Response Status
- **200 OK:** Lấy danh sách công ty thành công.
- **400 Bad Request:** Token không hợp lệ hoặc không tìm thấy thông tin người dùng.

#### Ví dụ Response thành công (200 OK)
```json
[
  {
    "id": 1,
    "name": "Công ty TNHH Công nghệ UniPath",
    "logoUrl": "https://cdn.unipath.com/logos/company-1.png",
    "roleInCompany": "OWNER",
    "isVerified": true
  }
]
```

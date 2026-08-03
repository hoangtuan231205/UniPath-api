# Tài liệu API Xác thực & Phân quyền (Authentication API)

Tài liệu chi tiết về các API đăng ký, đăng nhập và đổi mật khẩu cho dự án **UniPath API**.

---

## 📌 Thông tin chung
- **Base URL:** `/api/auth`
- **CORS:** Cho phép tất cả origins (`@CrossOrigin(origins = "*")`)
- **Định dạng dữ liệu:** `application/json`

---

## 📑 Danh sách API

### 1. Đăng ký tài khoản (`/api/auth/register`)
- **HTTP Method:** `POST`
- **URL Path:** `/api/auth/register`
- **Mô tả:** Đăng ký tài khoản người dùng mới (Ứng viên hoặc Nhà tuyển dụng).

#### Request Body
| Trường | Kiểu dữ liệu | Bắt buộc | Mặc định | Mô tả |
| :--- | :--- | :---: | :---: | :--- |
| `email` | `String` | Có | - | Địa chỉ email của người dùng |
| `password` | `String` | Có | - | Mật khẩu của tài khoản |
| `role` | `String` | Không | `"CANDIDATE"` | Vai trò tài khoản (`CANDIDATE` hoặc `EMPLOYER`) |

#### Ví dụ Request
```json
{
  "email": "user@example.com",
  "password": "Password123!",
  "role": "CANDIDATE"
}
```

---

### 2. Đăng nhập (`/api/auth/login`)
- **HTTP Method:** `POST`
- **URL Path:** `/api/auth/login`
- **Mô tả:** Đăng nhập hệ thống và nhận Token JWT.

#### Request Body
```json
{
  "email": "user@example.com",
  "password": "Password123!"
}
```

#### Ví dụ Response (200 OK)
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "userId": "1",
  "role": "CANDIDATE",
  "message": "Đăng nhập thành công!"
}
```

---

### 3. Đổi mật khẩu (`/api/auth/change-password`)
- **HTTP Method:** `POST`
- **URL Path:** `/api/auth/change-password`
- **Mô tả:** Đổi mật khẩu người dùng (Yêu cầu Header `Authorization: Bearer <token>`).

#### Request Body (ChangePasswordRequest)
| Trường | Kiểu dữ liệu | Bắt buộc | Mô tả |
| :--- | :--- | :---: | :--- |
| `oldPassword` | `String` | Có | Mật khẩu hiện tại của tài khoản |
| `newPassword` | `String` | Có | Mật khẩu mới cần thay đổi |

#### Ví dụ Request
```json
{
  "oldPassword": "123456",
  "newPassword": "newPassword123!"
}
```

#### Response Status
- **200 OK:** `Đổi mật khẩu thành công!`
- **400 Bad Request:** `Mật khẩu cũ không chính xác!` hoặc chưa xác thực token.

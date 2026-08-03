# Tài liệu API Quản lý Hồ sơ Người dùng (Profile API)

Tài liệu chi tiết các API quản lý thông tin hồ sơ cho cả **Ứng viên (Candidate)** và **Nhà tuyển dụng (Employer)** trong dự án **UniPath API**.

---

## 📌 Thông tin chung
- **Base URL:** `/api/profiles`
- **CORS:** Cho phép tất cả origins (`@CrossOrigin(origins = "*")`)
- **Định dạng dữ liệu:** `application/json`

---

## 👨‍🎓 1. API Dành Cho Ứng Viên (Candidate)

### 1.1 Lấy thông tin hồ sơ Ứng viên (`GET /api/profiles/candidate/{userId}`)
- **HTTP Method:** `GET`
- **URL Path:** `/api/profiles/candidate/{userId}`
- **Mô tả:** Lấy thông tin chi tiết hồ sơ ứng viên theo `userId`.

#### Path Variables
| Variable | Kiểu dữ liệu | Bắt buộc | Mô tả |
| :--- | :--- | :---: | :--- |
| `userId` | `Integer` | Có | ID của người dùng (User ID) |

#### Response Structure (CandidateProfileResponse)
| Trường | Kiểu dữ liệu | Mô tả |
| :--- | :--- | :--- |
| `id` | `Integer` | User ID của ứng viên |
| `email` | `String` | Email tài khoản |
| `fullName` | `String` | Họ và tên ứng viên |
| `phoneNumber` | `String` | Số điện thoại liên hệ |
| `experienceYears` | `Integer` | Số năm kinh nghiệm làm việc |
| `universityName` | `String` | Tên trường đại học đã/đang theo học |
| `major` | `String` | Chuyên ngành học |
| `skills` | `String` | Danh sách kỹ năng chuyên môn |

#### Response Status
- **200 OK:** Lấy hồ sơ thành công.
- **400 Bad Request:** Không tìm thấy User hoặc User không phải là vai trò `CANDIDATE`.

#### Ví dụ Response thành công (200 OK)
```json
{
  "id": 1,
  "email": "candidate@example.com",
  "fullName": "Nguyễn Văn A",
  "phoneNumber": "0987654321",
  "experienceYears": 2,
  "universityName": "Đại học Bách Khoa",
  "major": "Công nghệ thông tin",
  "skills": "Java, Spring Boot, PostgreSQL, React"
}
```

---

### 1.2 Cập nhật hồ sơ Ứng viên (`PUT /api/profiles/candidate/{userId}`)
- **HTTP Method:** `PUT`
- **URL Path:** `/api/profiles/candidate/{userId}`
- **Mô tả:** Cập nhật thông tin hồ sơ ứng viên.

#### Path Variables
| Variable | Kiểu dữ liệu | Bắt buộc | Mô tả |
| :--- | :--- | :---: | :--- |
| `userId` | `Integer` | Có | ID của người dùng |

#### Request Body (CandidateProfileRequest)
| Trường | Kiểu dữ liệu | Bắt buộc | Ràng buộc Validation | Mô tả |
| :--- | :--- | :---: | :--- | :--- |
| `fullName` | `String` | Có | `@NotBlank` ("Họ tên không được để trống") | Họ và tên đầy đủ |
| `phoneNumber` | `String` | Có | `@NotBlank` ("Số điện thoại không được để trống") | Số điện thoại |
| `experienceYears` | `Integer` | Có | `@NotNull` ("Số năm kinh nghiệm không được để trống") | Số năm kinh nghiệm |
| `skills` | `String` | Có | `@NotBlank` ("Kỹ năng không được để trống") | Mô tả kỹ năng |
| `universityName` | `String` | Không | - | Tên trường đại học |
| `major` | `String` | Không | - | Chuyên ngành |

#### Ví dụ Request Body
```json
{
  "fullName": "Nguyễn Văn A",
  "phoneNumber": "0987654321",
  "experienceYears": 2,
  "skills": "Java, Spring Boot, PostGIS",
  "universityName": "Đại học Bách Khoa",
  "major": "Kỹ thuật Phần mềm"
}
```

#### Response Status
- **200 OK:** Cập nhật thành công, trả về `CandidateProfileResponse` mới nhất.
- **400 Bad Request:** Lỗi validation các trường bắt buộc, không tìm thấy User hoặc vai trò không phải `CANDIDATE`.

---

## 🏢 2. API Dành Cho Nhà Tuyển Dụng (Employer)

### 2.1 Lấy thông tin hồ sơ Nhà tuyển dụng (`GET /api/profiles/employer/{userId}`)
- **HTTP Method:** `GET`
- **URL Path:** `/api/profiles/employer/{userId}`
- **Mô tả:** Lấy thông tin hồ sơ đại diện nhà tuyển dụng theo `userId`.

#### Path Variables
| Variable | Kiểu dữ liệu | Bắt buộc | Mô tả |
| :--- | :--- | :---: | :--- |
| `userId` | `Integer` | Có | ID của người dùng |

#### Response Structure (EmployerProfileResponse)
| Trường | Kiểu dữ liệu | Mô tả |
| :--- | :--- | :--- |
| `id` | `Integer` | User ID của nhà tuyển dụng |
| `email` | `String` | Email đăng ký |
| `fullName` | `String` | Họ tên người đại diện |
| `phone` | `String` | Số điện thoại |
| `position` | `String` | Chức danh / Vị trí công tác (vd: HR Manager, Tech Lead) |
| `bio` | `String` | Giới thiệu ngắn về nhà tuyển dụng |
| `isVerified` | `Boolean` | Trạng thái xác minh tài khoản (mặc định: `false`) |

#### Response Status
- **200 OK:** Lấy hồ sơ thành công.
- **400 Bad Request:** Không tìm thấy User hoặc User không phải là vai trò `EMPLOYER`.

#### Ví dụ Response thành công (200 OK)
```json
{
  "id": 2,
  "email": "hr@company.com",
  "fullName": "Trần Thị B",
  "phone": "0912345678",
  "position": "Trưởng phòng Nhân sự",
  "bio": "Đại diện tuyển dụng Công ty ABC",
  "isVerified": true
}
```

---

### 2.2 Cập nhật hồ sơ Nhà tuyển dụng (`PUT /api/profiles/employer/{userId}`)
- **HTTP Method:** `PUT`
- **URL Path:** `/api/profiles/employer/{userId}`
- **Mô tả:** Cập nhật thông tin hồ sơ người đại diện nhà tuyển dụng. *(Lưu ý: Trường `isVerified` do Admin/Hệ thống xử lý, người dùng không thể tự chỉnh sửa).*

#### Path Variables
| Variable | Kiểu dữ liệu | Bắt buộc | Mô tả |
| :--- | :--- | :---: | :--- |
| `userId` | `Integer` | Có | ID của người dùng |

#### Request Body (EmployerProfileRequest)
| Trường | Kiểu dữ liệu | Bắt buộc | Mô tả |
| :--- | :--- | :---: | :--- |
| `fullName` | `String` | Không | Họ tên đầy đủ |
| `phone` | `String` | Không | Số điện thoại liên hệ |
| `position` | `String` | Không | Vị trí / Chức vụ |
| `bio` | `String` | Không | Tiểu sử / Giới thiệu |

#### Ví dụ Request Body
```json
{
  "fullName": "Trần Thị B",
  "phone": "0912345678",
  "position": "Senior HR Manager",
  "bio": "Phụ trách tuyển dụng khối Công nghệ Thông tin"
}
```

#### Response Status
- **200 OK:** Cập nhật thành công, trả về `EmployerProfileResponse`.
- **400 Bad Request:** Không tìm thấy User hoặc vai trò không phải `EMPLOYER`.

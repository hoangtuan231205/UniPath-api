# Tài liệu API Quản lý Ca Làm việc & Nhân viên (Employment API)

Tài liệu chi tiết các API dành cho Nhà tuyển dụng / Quản lý doanh nghiệp trong việc phân ca, quản lý danh sách nhân viên và tính bảng lương trong **UniPath API**.

---

## 📌 Thông tin chung
- **Base URL:** `/api/employer`
- **CORS:** Cho phép tất cả origins (`@CrossOrigin(origins = "*")`)
- **Xác thực:** Yêu cầu Header `Authorization: Bearer <token>` có vai trò `EMPLOYER` hoặc đại diện doanh nghiệp.
- **Định dạng dữ liệu:** `application/json`

---

## 📑 Danh sách API

### 1. Lấy danh sách ca làm việc hôm nay (`GET /api/employer/shifts/today`)
- **HTTP Method:** `GET`
- **URL Path:** `/api/employer/shifts/today`
- **Mô tả:** Lấy danh sách tất cả các ca làm việc được phân trong ngày hôm nay của các nhân viên thuộc doanh nghiệp mà user quản lý.

#### Response Status
- **200 OK:** Lấy danh sách ca thành công.

---

### 2. Tạo ca làm việc mới (`POST /api/employer/shifts`)
- **HTTP Method:** `POST`
- **URL Path:** `/api/employer/shifts`
- **Mô tả:** Tạo một ca làm việc mới cho nhân viên.

#### Request Body (ShiftRequest)
| Trường | Kiểu dữ liệu | Bắt buộc | Ràng buộc Validation | Mô tả |
| :--- | :--- | :---: | :--- | :--- |
| `employmentId` | `Integer` | Có | `@NotNull` | ID của hợp đồng / mối quan hệ nhân viên |
| `shiftDate` | `LocalDate` | Có | `@NotNull` | Ngày làm việc (`YYYY-MM-DD`) |
| `startTime` | `LocalTime` | Có | `@NotNull` | Giờ bắt đầu (`HH:mm:ss`) |
| `endTime` | `LocalTime` | Có | `@NotNull` | Giờ kết thúc (`HH:mm:ss`) |
| `status` | `String` | Không | - | Trạng thái ca làm việc (vd: `SCHEDULED`, `COMPLETED`, `CANCELLED`) |
| `locationId` | `Integer` | Không | - | ID địa điểm / chi nhánh làm việc |

#### Ví dụ Request
```json
{
  "employmentId": 5,
  "shiftDate": "2026-08-10",
  "startTime": "08:00:00",
  "endTime": "12:00:00",
  "status": "SCHEDULED",
  "locationId": 1
}
```

#### Response Status
- **200 OK:** Tạo ca làm việc thành công.
- **400 Bad Request:** Thiếu trường bắt buộc hoặc không tìm thấy hợp đồng nhân viên.

---

### 3. Cập nhật ca làm việc (`PUT /api/employer/shifts/{id}`)
- **HTTP Method:** `PUT`
- **URL Path:** `/api/employer/shifts/{id}`
- **Mô tả:** Cập nhật thông tin thời gian hoặc trạng thái của ca làm việc theo `id`.

#### Path Variables
| Variable | Kiểu dữ liệu | Bắt buộc | Mô tả |
| :--- | :--- | :---: | :--- |
| `id` | `Integer` | Có | ID của ca làm việc cần sửa |

#### Request Body (ShiftRequest)
*(Tương tự Request Body của API Tạo ca làm việc)*

#### Response Status
- **200 OK:** Cập nhật ca làm việc thành công.
- **400 Bad Request:** Ca làm việc không tồn tại hoặc không hợp lệ.

---

### 4. Thêm nhân viên mới (`POST /api/employer/employees`)
- **HTTP Method:** `POST`
- **URL Path:** `/api/employer/employees`
- **Mô tả:** Thiết lập hợp đồng / nhận nhân viên mới vào công ty.

#### Request Body (EmploymentRequest)
| Trường | Kiểu dữ liệu | Bắt buộc | Ràng buộc Validation | Mô tả |
| :--- | :--- | :---: | :--- | :--- |
| `candidateId` | `Integer` | Có | `@NotNull` | ID của ứng viên |
| `baseSalaryPerHour` | `BigDecimal` | Không | - | Mức lương cơ bản theo giờ |
| `startDate` | `LocalDate` | Không | - | Ngày bắt đầu làm việc (`YYYY-MM-DD`) |

#### Ví dụ Request
```json
{
  "candidateId": 12,
  "baseSalaryPerHour": 35000.00,
  "startDate": "2026-08-01"
}
```

#### Response Status
- **200 OK:** Tạo hồ sơ nhân viên thành công.
- **400 Bad Request:** Thiếu ID ứng viên hoặc ứng viên đã là nhân viên.

---

### 5. Danh sách nhân viên công ty (`GET /api/employer/employees`)
- **HTTP Method:** `GET`
- **URL Path:** `/api/employer/employees`
- **Mô tả:** Lấy danh sách toàn bộ nhân viên đang làm việc tại doanh nghiệp do nhà tuyển dụng quản lý.

#### Response Status
- **200 OK:** Trả về danh sách nhân viên kèm thông tin chi tiết.

---

### 6. Bảng lương nhân viên (`GET /api/employer/payroll`)
- **HTTP Method:** `GET`
- **URL Path:** `/api/employer/payroll`
- **Mô tả:** Lấy tổng hợp bảng tính lương nhân viên theo tháng và năm.

#### Query Parameters
| Parameter | Kiểu dữ liệu | Bắt buộc | Mô tả |
| :--- | :--- | :---: | :--- |
| `month` | `Short` | Không | Tháng cần tính lương (1 - 12) |
| `year` | `Short` | Không | Năm cần tính lương (vd: 2026) |

#### Ví dụ Request URL
```http
GET /api/employer/payroll?month=8&year=2026
```

#### Response Status
- **200 OK:** Trả về danh sách bảng lương chi tiết từng nhân viên theo tổng giờ làm và mức lương cơ bản.

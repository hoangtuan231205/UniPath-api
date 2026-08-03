# Tài liệu API Vị trí Công ty (Company Location API)

Tài liệu chi tiết về các API quản lý tọa độ địa lý công ty và tìm kiếm công ty lân cận theo bán kính cho dự án **UniPath API**.

---

## 📌 Thông tin chung
- **Base URL:** `/api/companies`
- **Hệ tọa độ:** WGS84 (SRID 4326) - `[longitude, latitude]`
- **Định dạng dữ liệu:** `application/json`

---

## 📑 Danh sách API

### 1. Thêm vị trí địa lý cho công ty (`/api/companies/add`)
- **HTTP Method:** `POST`
- **URL Path:** `/api/companies/add`
- **Mô tả:** Khai báo vị trí địa chỉ và tọa độ địa lý (vĩ độ, kinh độ) cho một công ty.

#### Request Body (CompanyLocationRequest)
| Trường | Kiểu dữ liệu | Bắt buộc | Mô tả |
| :--- | :--- | :---: | :--- |
| `companyId` | `Integer` | Có | ID của công ty cần gán vị trí |
| `address` | `String` | Không | Địa chỉ chi tiết dạng văn bản |
| `lat` | `double` | Có | Vĩ độ (Latitude, ví dụ: `10.776889`) |
| `lon` | `double` | Có | Kinh độ (Longitude, ví dụ: `106.700806`) |

#### Ví dụ Request
```json
{
  "companyId": 1,
  "address": "720A Điện Biên Phủ, Phường 22, Bình Thạnh, TP.HCM",
  "lat": 10.795123,
  "lon": 106.721845
}
```

#### Response Structure (CompanyLocationResponse)
| Trường | Kiểu dữ liệu | Mô tả |
| :--- | :--- | :--- |
| `id` | `Integer` | ID bản ghi vị trí vừa tạo |
| `companyId` | `Integer` | ID của công ty |
| `address` | `String` | Địa chỉ công ty |
| `lat` | `double` | Vĩ độ đã lưu |
| `lon` | `double` | Kinh độ đã lưu |

#### Response Status
- **200 OK:** Tạo vị trí công ty thành công. Trả về đối tượng `CompanyLocationResponse`.
- **400 Bad Request:** Lỗi dữ liệu đầu vào (thiếu ID công ty hoặc không tìm thấy công ty tương ứng).

#### Ví dụ Response thành công (200 OK)
```json
{
  "id": 10,
  "companyId": 1,
  "address": "720A Điện Biên Phủ, Phường 22, Bình Thạnh, TP.HCM",
  "lat": 10.795123,
  "lon": 106.721845
}
```

#### Ví dụ Response lỗi (400 Bad Request)
```text
Phải cung cấp ID Công ty!
```
hoặc
```text
Không tìm thấy Công ty với ID: 999
```

---

### 2. Tìm kiếm công ty lân cận (`/api/companies/nearby`)
- **HTTP Method:** `GET`
- **URL Path:** `/api/companies/nearby`
- **Mô tả:** Tìm danh sách các công ty nằm trong bán kính xung quanh một tọa độ cho trước (sử dụng truy vấn không gian PostGIS).

#### Query Parameters
| Parameter | Kiểu dữ liệu | Bắt buộc | Giá trị mặc định | Mô tả |
| :--- | :--- | :---: | :---: | :--- |
| `lat` | `double` | Có | - | Vĩ độ của vị trí tìm kiếm |
| `lon` | `double` | Có | - | Kinh độ của vị trí tìm kiếm |
| `radius` | `double` | Không | `5000` | Bán kính tìm kiếm (tính bằng mét, ví dụ: 5000 = 5km) |

#### Ví dụ Request URL
```http
GET /api/companies/nearby?lat=10.795000&lon=106.721000&radius=2000
```

#### Response
- **200 OK:** Danh sách các vị trí công ty nằm trong bán kính tìm kiếm (`List<CompanyLocationResponse>`). Trả về mảng rỗng `[]` nếu không có công ty nào phù hợp.

#### Ví dụ Response thành công (200 OK)
```json
[
  {
    "id": 10,
    "companyId": 1,
    "address": "720A Điện Biên Phủ, Phường 22, Bình Thạnh, TP.HCM",
    "lat": 10.795123,
    "lon": 106.721845
  }
]
```

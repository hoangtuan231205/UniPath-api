# Tài liệu API Thông báo (Notification API)

Tài liệu chi tiết các API xem và đánh dấu đã đọc thông báo hệ thống cho người dùng trong dự án **UniPath API**.

---

## 📌 Thông tin chung
- **Base URL:** `/api/notifications`
- **Xác thực:** Yêu cầu Header `Authorization: Bearer <token>`.

---

## 📑 Danh sách API

### 1. Danh sách thông báo của tôi (`GET /api/notifications`)
- **HTTP Method:** `GET`
- **URL Path:** `/api/notifications?unreadOnly=`
- **Mô tả:** Lấy danh sách các thông báo cá nhân (vd: khi hồ sơ ứng tuyển được duyệt/từ chối).

#### Query Parameters
| Parameter | Kiểu dữ liệu | Bắt buộc | Mặc định | Mô tả |
| :--- | :--- | :---: | :---: | :--- |
| `unreadOnly` | `boolean` | Không | `false` | Nếu `true`, chỉ lấy thông báo chưa đọc |

#### Response Structure (List<NotificationResponse>)
```json
[
  {
    "id": 10,
    "userId": 1,
    "title": "Cập nhật trạng thái ứng tuyển",
    "message": "Hồ sơ ứng tuyển vị trí 'Lập trình viên Java' của bạn đã được chuyển sang trạng thái: ACCEPTED",
    "isRead": false,
    "createdAt": "2026-07-28T10:15:00"
  }
]
```

---

### 2. Đánh dấu 1 thông báo là đã đọc (`PATCH /api/notifications/{id}/read`)
- **HTTP Method:** `PATCH`
- **URL Path:** `/api/notifications/{id}/read`

---

### 3. Đánh dấu tất cả thông báo là đã đọc (`PATCH /api/notifications/read-all`)
- **HTTP Method:** `PATCH`
- **URL Path:** `/api/notifications/read-all`
- **Mô tả:** Chuyển `isRead = true` cho toàn bộ các thông báo của người dùng hiện tại.

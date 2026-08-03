# Tài liệu API Quản lý Ứng tuyển & Hồ sơ CV (Application API)

Tài liệu chi tiết các API ứng tuyển công việc, kiểm tra trạng thái ứng tuyển, quản lý hồ sơ ứng viên đối với nhà tuyển dụng và nộp/tải file CV cho dự án **UniPath API**.

---

## 📌 Thông tin chung
- **Base URL:** `/api/applications`
- **Xác thực:** Yêu cầu Header `Authorization: Bearer <token>`.
- **Hỗ trợ Upload File CV:** Multipart Form Data (`multipart/form-data`) với các định dạng `.pdf`, `.docx`, `.doc` (Kích thước tối đa 5MB).

---

## 📑 Danh sách API

### 1. Kiểm tra trạng thái đã ứng tuyển (`GET /api/jobs/{id}/check-applied`)
- **HTTP Method:** `GET`
- **URL Path:** `/api/jobs/{id}/check-applied`
- **Mô tả:** Kiểm tra Ứng viên hiện tại đã nộp hồ sơ vào công việc này chưa.

#### Response Structure (CheckAppliedResponse)
```json
{
  "applied": true,
  "appliedAt": "2026-07-28T09:00:00",
  "status": "SUBMITTED"
}
```

---

### 2. Nộp hồ sơ ứng tuyển (`POST /api/applications`)
- **HTTP Method:** `POST`
- **URL Path:** `/api/applications`
- **Content-Type:** `multipart/form-data` hoặc `application/json`
- **Mô tả:** Ứng viên nộp hồ sơ ứng tuyển vị trí công việc.

#### Request Parts / Body
- `data` (`ApplicationRequest` JSON):
  - `jobId` (`Integer`, bắt buộc): ID tin tuyển dụng
  - `cvProfileId` (`Integer`, không bắt buộc): ID CV mẫu AI
  - `coverLetter` (`String`, không bắt buộc): Thư xin việc
- `cvFile` (`MultipartFile`, không bắt buộc): File CV upload (PDF/DOCX, <= 5MB)

#### Ràng buộc Validation
- Kiểm tra trùng lặp: Mỗi ứng viên chỉ được ứng tuyển 1 lần per job. Nếu trùng sẽ trả về thông báo lỗi: `Bạn đã ứng tuyển vị trí này`.
- Kiểm tra định dạng file: Chỉ chấp nhận PDF, DOCX, DOC <= 5MB.

---

### 3. Rút hồ sơ ứng tuyển (`DELETE /api/applications/{id}`)
- **HTTP Method:** `DELETE`
- **URL Path:** `/api/applications/{id}`
- **Mô tả:** Ứng viên rút hồ sơ đã nộp.
- **Ràng buộc:** Chỉ cho phép rút hồ sơ khi `status == 'SUBMITTED'`.

---

### 4. Lịch sử ứng tuyển của tôi (`GET /api/applications/me`)
- **HTTP Method:** `GET`
- **URL Path:** `/api/applications/me?status=`
- **Mô tả:** Ứng viên xem danh sách lịch sử các công việc đã nộp hồ sơ (có thể lọc theo `status`).

---

### 5. Danh sách ứng viên theo Job (`GET /api/jobs/{id}/applications`)
- **HTTP Method:** `GET`
- **URL Path:** `/api/jobs/{id}/applications?status=`
- **Mô tả:** Nhà tuyển dụng xem danh sách các ứng viên đã nộp vào tin tuyển dụng của công ty mình.

---

### 6. Cập nhật trạng thái ứng tuyển (`PATCH /api/applications/{id}/status`)
- **HTTP Method:** `PATCH`
- **URL Path:** `/api/applications/{id}/status`
- **Mô tả:** Nhà tuyển dụng đổi trạng thái ứng tuyển (vd: `ACCEPTED`, `REJECTED`, `INTERVIEW`). Tự động tạo 1 thông báo vào bảng `notifications` cho ứng viên.

#### Request Body (UpdateApplicationStatusRequest)
```json
{
  "status": "ACCEPTED",
  "note": "Mời bạn tham gia phỏng vấn lúc 9h sáng thứ 2 tuần sau"
}
```

---

### 7. Tải file CV (`GET /api/applications/{id}/cv/download`)
- **HTTP Method:** `GET`
- **URL Path:** `/api/applications/{id}/cv/download`
- **Mô tả:** Tải file CV đã đính kèm. Trả về stream file đính kèm dưới dạng Attachment.

package com.example.unipathapi.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Object> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Dữ liệu đầu vào không hợp lệ");
        body.put("message", "Vui lòng kiểm tra lại. Định dạng dữ liệu không phù hợp.");

        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public ResponseEntity<Object> handleResponseStatusException(org.springframework.web.server.ResponseStatusException ex) {
        org.springframework.http.HttpStatusCode statusCode = ex.getStatusCode();
        HttpStatus status = HttpStatus.resolve(statusCode.value());
        if (status == null) status = HttpStatus.BAD_REQUEST;

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", ex.getReason() != null ? ex.getReason() : ex.getMessage());

        return new ResponseEntity<>(body, status);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Object> handleIllegalStateException(IllegalStateException ex) {
        String msg = ex.getMessage() != null ? ex.getMessage() : "Xung đột trạng thái yêu cầu";
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.CONFLICT.value());
        body.put("error", HttpStatus.CONFLICT.getReasonPhrase());
        body.put("message", msg);

        return new ResponseEntity<>(body, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(com.example.unipathapi.auth.exception.GoogleServiceUnavailableException.class)
    public ResponseEntity<Object> handleGoogleServiceUnavailable(com.example.unipathapi.auth.exception.GoogleServiceUnavailableException ex) {
        String msg = ex.getMessage() != null ? ex.getMessage() : "Dịch vụ xác thực Google tạm thời không khả dụng";
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        body.put("error", HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase());
        body.put("message", msg);

        return new ResponseEntity<>(body, HttpStatus.SERVICE_UNAVAILABLE);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Object> handleRuntimeException(RuntimeException ex) {
        String msg = ex.getMessage() != null ? ex.getMessage() : "Lỗi xử lý yêu cầu";
        HttpStatus status = HttpStatus.BAD_REQUEST;

        if (msg.startsWith("403:") || msg.contains("khoá") || msg.contains("khóa")) {
            status = HttpStatus.FORBIDDEN;
            if (msg.startsWith("403:")) {
                msg = msg.substring(4).trim();
            }
        } else if (msg.startsWith("404:")) {
            status = HttpStatus.NOT_FOUND;
            msg = msg.substring(4).trim();
        } else if (msg.startsWith("400:")) {
            status = HttpStatus.BAD_REQUEST;
            msg = msg.substring(4).trim();
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", msg);

        return new ResponseEntity<>(body, status);
    }

    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleMethodArgumentNotValid(org.springframework.web.bind.MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (org.springframework.validation.FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Dữ liệu không hợp lệ");
        body.put("message", "Vui lòng kiểm tra lại các trường thông tin");
        body.put("errors", fieldErrors);

        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<Object> handleHttpMessageNotReadable(org.springframework.http.converter.HttpMessageNotReadableException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Bad Request");
        String message = "Dữ liệu gửi lên không đúng định dạng hoặc giá trị không hợp lệ";
        Throwable root = ex.getRootCause();
        if (root != null && root.getMessage() != null) {
            String rootMsg = root.getMessage();
            if (rootMsg.contains("JobType")) {
                message = "Giá trị loại hình công việc (jobType) không hợp lệ. Các giá trị được chấp nhận: FULL_TIME, PART_TIME, INTERNSHIP, CONTRACT";
            } else if (rootMsg.contains("WorkMode")) {
                message = "Giá trị hình thức làm việc (workMode) không hợp lệ. Các giá trị được chấp nhận: ONSITE, REMOTE, HYBRID";
            }
        }
        body.put("message", message);
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<Object> handleDataIntegrityViolation(org.springframework.dao.DataIntegrityViolationException ex) {
        String rootMsg = ex.getRootCause() != null ? ex.getRootCause().getMessage() : ex.getMessage();
        String lowerMsg = rootMsg != null ? rootMsg.toLowerCase() : "";

        HttpStatus status = HttpStatus.BAD_REQUEST;
        String userFriendlyMsg = "Dữ liệu vi phạm ràng buộc hệ thống. Vui lòng kiểm tra lại.";

        if (lowerMsg.contains("company_locations_google_place_id_unique")) {
            status = HttpStatus.CONFLICT;
            userFriendlyMsg = "Google Place ID này đã được đăng ký cho một địa điểm khác.";
        } else if (lowerMsg.contains("company_locations_one_primary_per_company")) {
            status = HttpStatus.CONFLICT;
            userFriendlyMsg = "Công ty đã có địa điểm chính đang được cập nhật. Vui lòng thử lại.";
        } else if (lowerMsg.contains("users_email_key") || lowerMsg.contains("uq_auth_provider") || lowerMsg.contains("user_auth_providers")) {
            status = HttpStatus.CONFLICT;
            userFriendlyMsg = "Email hoặc tài khoản Google này đã được đăng ký trong hệ thống.";
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", userFriendlyMsg);

        return new ResponseEntity<>(body, status);
    }

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGeneralException(Exception ex) {
        log.error("Unhandled internal server error: ", ex);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        body.put("error", "Lỗi máy chủ");

        String msg = ex.getMessage();
        if (msg == null || msg.toLowerCase().contains("sql") || msg.toLowerCase().contains("hibernate") || msg.toLowerCase().contains("could not execute statement") || msg.toLowerCase().contains("syntax error")) {
            msg = "Đã xảy ra lỗi nội bộ máy chủ. Vui lòng liên hệ quản trị viên.";
        }
        body.put("message", msg);

        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}

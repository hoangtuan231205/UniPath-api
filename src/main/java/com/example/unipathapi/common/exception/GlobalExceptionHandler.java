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

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Object> handleRuntimeException(RuntimeException ex) {
        String msg = ex.getMessage() != null ? ex.getMessage() : "Lỗi xử lý yêu cầu";
        HttpStatus status = HttpStatus.BAD_REQUEST;

        if (msg.startsWith("403:")) {
            status = HttpStatus.FORBIDDEN;
            msg = msg.substring(4).trim();
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
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", userFriendlyMsg);

        return new ResponseEntity<>(body, status);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGeneralException(Exception ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        body.put("error", "Lỗi máy chủ");
        body.put("message", ex.getMessage());

        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}

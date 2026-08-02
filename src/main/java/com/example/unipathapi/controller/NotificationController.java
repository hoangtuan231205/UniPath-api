package com.example.unipathapi.controller;

import com.example.unipathapi.service.NotificationService;
import com.example.unipathapi.util.SecurityUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "*")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private SecurityUtil securityUtil;

    @GetMapping
    public ResponseEntity<?> getMyNotifications(@RequestParam(defaultValue = "false") boolean unreadOnly, HttpServletRequest request) {
        try {
            Integer userId = securityUtil.getCurrentUserId(request);
            return ResponseEntity.ok(notificationService.getMyNotifications(userId, unreadOnly));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(@PathVariable Integer id, HttpServletRequest request) {
        try {
            Integer userId = securityUtil.getCurrentUserId(request);
            return ResponseEntity.ok(notificationService.markAsRead(id, userId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PatchMapping("/read-all")
    public ResponseEntity<?> markAllAsRead(HttpServletRequest request) {
        try {
            Integer userId = securityUtil.getCurrentUserId(request);
            notificationService.markAllAsRead(userId);
            return ResponseEntity.ok("Đã đánh dấu tất cả thông báo là đã đọc");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}

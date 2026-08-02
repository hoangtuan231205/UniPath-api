package com.example.unipathapi.service;

import com.example.unipathapi.dto.response.NotificationResponse;
import com.example.unipathapi.entity.Notification;
import com.example.unipathapi.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    public List<NotificationResponse> getMyNotifications(Integer userId, boolean unreadOnly) {
        List<Notification> notifications = notificationRepository.findByUserIdAndUnreadFilter(userId, unreadOnly);
        return notifications.stream().map(this::buildNotificationResponse).collect(Collectors.toList());
    }

    public NotificationResponse markAsRead(Integer id, Integer userId) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Thông báo không tồn tại"));

        if (!notification.getUser().getId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền cập nhật thông báo này");
        }

        notification.setIsRead(true);
        Notification saved = notificationRepository.save(notification);
        return buildNotificationResponse(saved);
    }

    @Transactional
    public void markAllAsRead(Integer userId) {
        notificationRepository.markAllAsReadByUserId(userId);
    }

    private NotificationResponse buildNotificationResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .userId(n.getUser().getId())
                .title(n.getTitle())
                .message(n.getMessage())
                .isRead(n.getIsRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}

package com.example.unipathapi.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class NotificationResponse {
    private Integer id;
    private Integer userId;
    private String title;
    private String message;
    private Boolean isRead;
    private LocalDateTime createdAt;
}

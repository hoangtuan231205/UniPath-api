package com.example.unipathapi.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class AdminAuditLogResponse {
    private Integer id;
    private Integer adminId;
    private String adminEmail;
    private String action;
    private String targetType;
    private Integer targetId;
    private String detail;
    private LocalDateTime createdAt;
}

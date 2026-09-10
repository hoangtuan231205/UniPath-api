package com.example.unipathapi.admin.service;
import com.example.unipathapi.admin.entity.*;
import com.example.unipathapi.admin.repository.*;
import com.example.unipathapi.admin.dto.request.*;
import com.example.unipathapi.admin.dto.response.*;
import com.example.unipathapi.admin.service.*;

import com.example.unipathapi.admin.dto.response.AdminAuditLogResponse;
import com.example.unipathapi.admin.entity.AdminAuditLog;
import com.example.unipathapi.user.entity.User;
import com.example.unipathapi.admin.repository.AdminAuditLogRepository;
import com.example.unipathapi.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminAuditLogService {

    @Autowired
    private AdminAuditLogRepository auditLogRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public void log(Integer adminId, String action, String targetType, Integer targetId, String detail) {
        if (adminId == null) return;
        User admin = userRepository.findById(adminId).orElse(null);
        if (admin == null) return;

        AdminAuditLog log = new AdminAuditLog(admin, action, targetType, targetId, detail);
        auditLogRepository.save(log);
    }

    public List<AdminAuditLogResponse> getLogs(Integer adminId, String targetType, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1));
        String type = (targetType != null && !targetType.isBlank()) ? targetType.trim().toUpperCase() : null;
        Page<AdminAuditLog> logPage = auditLogRepository.findLogs(adminId, type, pageable);

        return logPage.getContent().stream()
                .map(this::buildResponse)
                .collect(Collectors.toList());
    }

    private AdminAuditLogResponse buildResponse(AdminAuditLog log) {
        return AdminAuditLogResponse.builder()
                .id(log.getId())
                .adminId(log.getAdmin().getId())
                .adminEmail(log.getAdmin().getEmail())
                .action(log.getAction())
                .targetType(log.getTargetType())
                .targetId(log.getTargetId())
                .detail(log.getDetail())
                .createdAt(log.getCreatedAt())
                .build();
    }
}

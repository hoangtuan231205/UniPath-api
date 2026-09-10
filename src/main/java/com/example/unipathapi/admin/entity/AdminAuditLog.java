package com.example.unipathapi.admin.entity;
import com.example.unipathapi.user.entity.User;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "admin_audit_logs")
public class AdminAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id", nullable = false)
    private User admin;

    @Column(nullable = false, length = 50)
    private String action;

    @Column(name = "target_type", nullable = false, length = 30)
    private String targetType;

    @Column(name = "target_id", nullable = false)
    private Integer targetId;

    @Column(columnDefinition = "TEXT")
    private String detail;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public AdminAuditLog(User admin, String action, String targetType, Integer targetId, String detail) {
        this.admin = admin;
        this.action = action;
        this.targetType = targetType;
        this.targetId = targetId;
        this.detail = detail;
    }
}

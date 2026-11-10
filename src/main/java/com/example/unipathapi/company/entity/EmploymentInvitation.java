package com.example.unipathapi.company.entity;

import com.example.unipathapi.application.entity.Application;
import com.example.unipathapi.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "employment_invitations")
public class EmploymentInvitation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    private User candidate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id")
    private Application application;

    @Column(name = "invitation_type", nullable = false, length = 20)
    private String invitationType; // 'RECRUITMENT', 'DIRECT'

    @Column(name = "status", nullable = false, length = 20)
    private String status = "PENDING"; // 'PENDING', 'ACCEPTED', 'DECLINED', 'CANCELLED', 'EXPIRED'

    @Column(name = "position", nullable = false)
    private String position;

    @Column(name = "salary_type", nullable = false, length = 20)
    private String salaryType = "HOURLY"; // 'HOURLY', 'MONTHLY', 'NEGOTIABLE', 'UNPAID'

    @Column(name = "base_salary_per_hour", precision = 10, scale = 2)
    private BigDecimal baseSalaryPerHour;

    @Column(name = "base_salary_monthly", precision = 12, scale = 2)
    private BigDecimal baseSalaryMonthly;

    @Column(name = "salary_note")
    private String salaryNote;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "note")
    private String note;

    @Column(name = "decline_reason")
    private String declineReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invited_by", nullable = false)
    private User invitedBy;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
}

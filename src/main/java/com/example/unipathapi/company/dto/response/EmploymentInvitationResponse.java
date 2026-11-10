package com.example.unipathapi.company.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class EmploymentInvitationResponse {
    private Integer id;
    private Integer companyId;
    private String companyName;
    private String companyLogoUrl;
    private Integer candidateId;
    private String candidateName;
    private String candidateEmail;
    private Integer applicationId;
    private String invitationType; // 'RECRUITMENT', 'DIRECT'
    private String status;         // 'PENDING', 'ACCEPTED', 'DECLINED', 'CANCELLED', 'EXPIRED'
    private String position;
    private String salaryType;     // 'HOURLY', 'MONTHLY', 'NEGOTIABLE', 'UNPAID'
    private BigDecimal baseSalaryPerHour;
    private BigDecimal baseSalaryMonthly;
    private String salaryNote;
    private LocalDate startDate;
    private String note;
    private String declineReason;
    private Integer invitedById;
    private String invitedByName;
    private LocalDateTime createdAt;
    private LocalDateTime respondedAt;
    private LocalDateTime expiresAt;
}

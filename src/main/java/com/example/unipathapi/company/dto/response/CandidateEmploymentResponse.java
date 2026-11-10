package com.example.unipathapi.company.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Builder
public class CandidateEmploymentResponse {
    private Integer employmentId;
    private Integer invitationId;
    private Boolean isEmployment;
    private Integer companyId;
    private String companyName;
    private String companyLogoUrl;
    private Boolean hrmEnabled;
    private String position;
    private String salaryType;
    private BigDecimal baseSalaryPerHour;
    private BigDecimal baseSalaryMonthly;
    private LocalDate startDate;
    private String status;
}

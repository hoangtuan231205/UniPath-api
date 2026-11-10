package com.example.unipathapi.company.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Builder
public class EmployeeResponse {
    private Integer employmentId;
    private Integer studentId;
    private String studentName;
    private String email;
    private String phone;
    private String universityName;
    private String major;
    private BigDecimal baseSalaryPerHour;
    private BigDecimal baseSalaryMonthly;
    private String salaryType;
    private String position;
    private Integer invitationId;
    private Integer companyId;
    private String companyName;
    private LocalDate startDate;
    private String status;
}

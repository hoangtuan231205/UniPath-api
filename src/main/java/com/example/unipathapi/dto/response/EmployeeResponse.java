package com.example.unipathapi.dto.response;

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
    private LocalDate startDate;
    private String status;
}

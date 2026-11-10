package com.example.unipathapi.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class UpdateApplicationStatusRequest {

    @NotBlank(message = "Trạng thái không được để trống")
    private String status;

    private LocalDateTime interviewAt;
    private String interviewLocation;
    private String note;

    private String salaryType;
    private java.math.BigDecimal baseSalaryPerHour;
    private java.math.BigDecimal baseSalaryMonthly;
    private String salaryNote;
    private java.time.LocalDate startDate;
    private String position;
}

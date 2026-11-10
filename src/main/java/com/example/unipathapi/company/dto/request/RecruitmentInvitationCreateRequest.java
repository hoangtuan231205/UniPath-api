package com.example.unipathapi.company.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class RecruitmentInvitationCreateRequest {

    @NotBlank(message = "Vị trí công việc không được để trống")
    private String position;

    @NotBlank(message = "Hình thức lương không được để trống")
    private String salaryType = "HOURLY"; // 'HOURLY', 'MONTHLY', 'NEGOTIABLE', 'UNPAID'

    private BigDecimal baseSalaryPerHour;

    private BigDecimal baseSalaryMonthly;

    private String salaryNote;

    @NotNull(message = "Ngày bắt đầu dự kiến không được để trống")
    private LocalDate startDate;

    private String note;
}

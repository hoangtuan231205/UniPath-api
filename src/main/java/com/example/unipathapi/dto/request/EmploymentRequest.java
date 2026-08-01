package com.example.unipathapi.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class EmploymentRequest {

    @NotNull(message = "ID ứng viên không được để trống")
    private Integer candidateId;

    private BigDecimal baseSalaryPerHour;
    private LocalDate startDate;
}

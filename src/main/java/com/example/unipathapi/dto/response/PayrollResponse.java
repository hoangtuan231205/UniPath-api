package com.example.unipathapi.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
public class PayrollResponse {
    private Integer id;
    private Integer employmentId;
    private String employeeName;
    private String monthYear;
    private Short payMonth;
    private Short payYear;
    private BigDecimal totalHours;
    private BigDecimal totalPenalties;
    private BigDecimal finalSalary;
    private String status;
}

package com.example.unipathapi.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@Table(name = "payroll")
public class Payroll {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employment_id")
    private Employment employment;

    @Column(name = "month_year", length = 10)
    private String monthYear;

    @Column(name = "total_hours", precision = 10, scale = 2)
    private BigDecimal totalHours;

    @Column(name = "total_penalties", precision = 10, scale = 2)
    private BigDecimal totalPenalties;

    @Column(name = "final_salary", precision = 12, scale = 2)
    private BigDecimal finalSalary;

    @Column(length = 50)
    private String status = "PENDING";

    @Column(name = "pay_month")
    private Short payMonth;

    @Column(name = "pay_year")
    private Short payYear;
}

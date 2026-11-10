package com.example.unipathapi.company.entity;
import com.example.unipathapi.user.entity.User;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Getter
@Setter
@Table(name = "employments")
public class Employment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id")
    private User candidate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    @Column(name = "base_salary_per_hour", precision = 10, scale = 2)
    private BigDecimal baseSalaryPerHour;

    @Column(name = "start_date")
    private LocalDate startDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invitation_id")
    private EmploymentInvitation invitation;

    @Column(name = "position")
    private String position;

    @Column(name = "salary_type", length = 20)
    private String salaryType = "HOURLY";

    @Column(name = "base_salary_monthly", precision = 12, scale = 2)
    private BigDecimal baseSalaryMonthly;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "termination_reason", columnDefinition = "TEXT")
    private String terminationReason;

    @Column(length = 50)
    private String status = "ACTIVE";
}

package com.example.unipathapi.repository;

import com.example.unipathapi.entity.Payroll;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PayrollRepository extends JpaRepository<Payroll, Integer> {

    @Query("SELECT p FROM Payroll p WHERE p.employment.company.id = :companyId " +
           "AND (:month IS NULL OR p.payMonth = :month) " +
           "AND (:year IS NULL OR p.payYear = :year)")
    List<Payroll> findPayrollByCompanyAndOptionalMonthYear(@Param("companyId") Integer companyId,
                                                           @Param("month") Short month,
                                                           @Param("year") Short year);
}

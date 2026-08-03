package com.example.unipathapi.repository;

import com.example.unipathapi.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, Integer> {

    @Query("SELECT r FROM Report r WHERE (:status IS NULL OR r.status = :status) ORDER BY r.createdAt DESC")
    List<Report> findByOptionalStatus(@Param("status") String status);
}

package com.example.unipathapi.admin.repository;
import com.example.unipathapi.admin.entity.*;

import com.example.unipathapi.admin.entity.Report;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, Integer> {

    @Query("SELECT r FROM Report r " +
           "LEFT JOIN FETCH r.reporter " +
           "LEFT JOIN FETCH r.job " +
           "LEFT JOIN FETCH r.post p " +
           "LEFT JOIN FETCH p.author " +
           "LEFT JOIN FETCH r.resolvedBy " +
           "WHERE (:targetType IS NULL OR r.targetType = :targetType) AND " +
           "(:status IS NULL OR r.status = :status) " +
           "ORDER BY r.createdAt DESC")
    List<Report> findByOptionalFilters(@Param("targetType") String targetType,
                                       @Param("status") String status);

    @Query(value = "SELECT r FROM Report r " +
                   "LEFT JOIN FETCH r.reporter " +
                   "LEFT JOIN FETCH r.job " +
                   "LEFT JOIN FETCH r.post p " +
                   "LEFT JOIN FETCH p.author " +
                   "LEFT JOIN FETCH r.resolvedBy " +
                   "WHERE (:targetType IS NULL OR r.targetType = :targetType) AND " +
                   "(:status IS NULL OR r.status = :status) " +
                   "ORDER BY r.createdAt DESC",
           countQuery = "SELECT COUNT(r) FROM Report r WHERE " +
                        "(:targetType IS NULL OR r.targetType = :targetType) AND " +
                        "(:status IS NULL OR r.status = :status)")
    Page<Report> findByOptionalFiltersPaged(@Param("targetType") String targetType,
                                            @Param("status") String status,
                                            Pageable pageable);

    boolean existsByReporterIdAndPostIdAndStatus(Integer reporterId, Integer postId, String status);

    List<Report> findByPostIdAndStatus(Integer postId, String status);
}

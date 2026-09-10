package com.example.unipathapi.admin.repository;
import com.example.unipathapi.admin.entity.*;

import com.example.unipathapi.admin.entity.AdminAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, Integer> {

    @Query("SELECT a FROM AdminAuditLog a WHERE " +
           "(:adminId IS NULL OR a.admin.id = :adminId) AND " +
           "(:targetType IS NULL OR a.targetType = :targetType) " +
           "ORDER BY a.createdAt DESC")
    Page<AdminAuditLog> findLogs(@Param("adminId") Integer adminId,
                                 @Param("targetType") String targetType,
                                 Pageable pageable);
}

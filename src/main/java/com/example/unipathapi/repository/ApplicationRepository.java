package com.example.unipathapi.repository;

import com.example.unipathapi.entity.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Integer> {

    Optional<Application> findTopByJobIdAndStudentIdOrderByAppliedAtDesc(Integer jobId, Integer studentId);

    boolean existsByJobIdAndStudentId(Integer jobId, Integer studentId);

    long countByJobId(Integer jobId);

    @Query("SELECT a FROM Application a WHERE a.student.id = :studentId AND (:status IS NULL OR a.status = :status) ORDER BY a.appliedAt DESC")
    List<Application> findByStudentIdAndOptionalStatus(@Param("studentId") Integer studentId, @Param("status") String status);

    @Query("SELECT a FROM Application a WHERE a.job.id = :jobId AND (:status IS NULL OR a.status = :status) ORDER BY a.appliedAt DESC")
    List<Application> findByJobIdAndOptionalStatus(@Param("jobId") Integer jobId, @Param("status") String status);

    long countByStatus(String status);
}

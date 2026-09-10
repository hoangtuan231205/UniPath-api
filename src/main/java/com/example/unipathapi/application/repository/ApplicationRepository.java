package com.example.unipathapi.application.repository;
import com.example.unipathapi.application.entity.*;

import com.example.unipathapi.application.entity.Application;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Integer> {

    Optional<Application> findTopByJobIdAndCandidateIdOrderByAppliedAtDesc(Integer jobId, Integer candidateId);

    boolean existsByJobIdAndCandidateId(Integer jobId, Integer candidateId);

    long countByJobId(Integer jobId);

    @Query("SELECT a FROM Application a WHERE a.candidate.id = :candidateId AND (:status IS NULL OR a.status = :status) ORDER BY a.appliedAt DESC")
    Page<Application> findByCandidateIdAndOptionalStatus(@Param("candidateId") Integer candidateId, @Param("status") String status, Pageable pageable);

    @Query("SELECT a FROM Application a WHERE a.job.company.id IN :companyIds " +
           "AND (:jobId IS NULL OR a.job.id = :jobId) " +
           "AND (:status IS NULL OR a.status = :status) ORDER BY a.appliedAt DESC")
    Page<Application> searchByCompanyIds(
            @Param("companyIds") Collection<Integer> companyIds,
            @Param("jobId") Integer jobId,
            @Param("status") String status,
            Pageable pageable
    );

    long countByStatus(String status);
}

package com.example.unipathapi.application.repository;
import com.example.unipathapi.application.entity.*;

import com.example.unipathapi.application.entity.Application;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.example.unipathapi.job.dto.response.JobApplicationStatsDto;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Integer> {

    Optional<Application> findTopByJobIdAndCandidateIdOrderByAppliedAtDesc(Integer jobId, Integer candidateId);

    boolean existsByJobIdAndCandidateId(Integer jobId, Integer candidateId);

    long countByJobId(Integer jobId);

    @Query("SELECT a FROM Application a WHERE a.candidate.id = :candidateId AND (:status IS NULL OR a.status = :status) ORDER BY a.appliedAt DESC")
    Page<Application> findByCandidateIdAndOptionalStatus(@Param("candidateId") Integer candidateId, @Param("status") String status, Pageable pageable);

    @Query(value = "SELECT a FROM Application a " +
           "LEFT JOIN FETCH a.job j " +
           "LEFT JOIN FETCH j.company c " +
           "LEFT JOIN FETCH j.location l " +
           "WHERE a.candidate.id = :candidateId " +
           "AND (:status IS NULL OR :status = '' OR a.status = :status) " +
           "AND (:search IS NULL OR :search = '' OR " +
           "     LOWER(j.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "     LOWER(c.companyName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "     LOWER(l.address) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "     LOWER(l.locationName) LIKE LOWER(CONCAT('%', :search, '%')))",
           countQuery = "SELECT COUNT(a) FROM Application a " +
           "LEFT JOIN a.job j " +
           "LEFT JOIN j.company c " +
           "LEFT JOIN j.location l " +
           "WHERE a.candidate.id = :candidateId " +
           "AND (:status IS NULL OR :status = '' OR a.status = :status) " +
           "AND (:search IS NULL OR :search = '' OR " +
           "     LOWER(j.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "     LOWER(c.companyName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "     LOWER(l.address) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "     LOWER(l.locationName) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Application> findCandidateApplications(
            @Param("candidateId") Integer candidateId,
            @Param("status") String status,
            @Param("search") String search,
            Pageable pageable
    );

    @Query("SELECT a.status, COUNT(a.id) FROM Application a WHERE a.candidate.id = :candidateId GROUP BY a.status")
    List<Object[]> countByStatusForCandidate(@Param("candidateId") Integer candidateId);

    @Query("SELECT a FROM Application a WHERE a.job.company.id IN :companyIds " +
           "AND (:jobId IS NULL OR a.job.id = :jobId) " +
           "AND (:status IS NULL OR a.status = :status) ORDER BY a.appliedAt DESC")
    Page<Application> searchByCompanyIds(
            @Param("companyIds") Collection<Integer> companyIds,
            @Param("jobId") Integer jobId,
            @Param("status") String status,
            Pageable pageable
    );

    @Query(value = "SELECT a FROM Application a " +
           "JOIN FETCH a.candidate c " +
           "WHERE a.job.id = :jobId " +
           "AND (:status IS NULL OR :status = '' OR a.status = :status) " +
           "ORDER BY a.appliedAt DESC",
           countQuery = "SELECT COUNT(a) FROM Application a " +
           "WHERE a.job.id = :jobId " +
           "AND (:status IS NULL OR :status = '' OR a.status = :status)")
    Page<Application> findByJobIdAndOptionalStatus(
            @Param("jobId") Integer jobId,
            @Param("status") String status,
            Pageable pageable
    );

    @Query("SELECT new com.example.unipathapi.job.dto.response.JobApplicationStatsDto(" +
           "a.job.id, " +
           "COUNT(a.id), " +
           "SUM(CASE WHEN a.status = 'SUBMITTED' THEN 1L ELSE 0L END), " +
           "SUM(CASE WHEN a.status = 'INTERVIEW' THEN 1L ELSE 0L END)) " +
           "FROM Application a " +
           "WHERE a.job.id IN :jobIds " +
           "GROUP BY a.job.id")
    List<JobApplicationStatsDto> getStatsForJobIds(@Param("jobIds") Collection<Integer> jobIds);

    @Query("SELECT COUNT(a) FROM Application a WHERE a.job.company.id IN :companyIds")
    long countByCompanyIds(@Param("companyIds") Collection<Integer> companyIds);

    long countByStatus(String status);

    @Query("SELECT a.job.id FROM Application a WHERE a.candidate.id = :candidateId AND a.job.id IN :jobIds")
    java.util.Set<Integer> findAppliedJobIdsByCandidateIdAndJobIds(@Param("candidateId") Integer candidateId, @Param("jobIds") Collection<Integer> jobIds);

    @Modifying
    @Query("UPDATE Application a SET a.cvProfileId = null WHERE a.cvProfileId = :cvProfileId")
    void detachCvProfileFromApplications(@Param("cvProfileId") Integer cvProfileId);
}

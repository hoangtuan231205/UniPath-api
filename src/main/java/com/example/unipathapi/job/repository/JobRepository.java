package com.example.unipathapi.job.repository;

import com.example.unipathapi.job.entity.Job;
import com.example.unipathapi.job.enums.JobType;
import com.example.unipathapi.job.enums.WorkMode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface JobRepository extends JpaRepository<Job, Integer> {

    @Query("SELECT j FROM Job j WHERE j.isActive = true " +
           "AND (:cursor IS NULL OR j.id < :cursor) " +
           "AND (CAST(:keyword AS string) IS NULL OR LOWER(j.title) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) OR j.description LIKE CONCAT('%', CAST(:keyword AS string), '%')) " +
           "AND (:categoryId IS NULL OR j.category.id = :categoryId) " +
           "AND (:locationId IS NULL OR j.location.id = :locationId) " +
           "AND (:jobType IS NULL OR j.jobType = :jobType) " +
           "AND (:workMode IS NULL OR j.workMode = :workMode) " +
           "ORDER BY j.postedAt DESC, j.id DESC")
    List<Job> findFeedJobs(@Param("cursor") Integer cursor,
                           @Param("keyword") String keyword,
                           @Param("categoryId") Integer categoryId,
                           @Param("locationId") Integer locationId,
                           @Param("jobType") JobType jobType,
                           @Param("workMode") WorkMode workMode,
                           Pageable pageable);

    List<Job> findByCompanyId(Integer companyId);

    long countByIsActiveTrue();

    boolean existsByCategoryId(Integer categoryId);

    long countByCompanyIdIn(Collection<Integer> companyIds);

    @Query("SELECT COUNT(j) FROM Job j WHERE j.company.id IN :companyIds " +
           "AND j.isActive = true AND (j.expiredAt IS NULL OR j.expiredAt >= CURRENT_TIMESTAMP)")
    long countActiveByCompanyIds(@Param("companyIds") Collection<Integer> companyIds);

    @Query("SELECT COUNT(j) FROM Job j WHERE j.company.id IN :companyIds AND j.isActive = false")
    long countClosedByCompanyIds(@Param("companyIds") Collection<Integer> companyIds);

    @Query("SELECT j FROM Job j WHERE j.company.id IN :companyIds " +
           "AND (CAST(:keyword AS string) IS NULL OR LOWER(j.title) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))) " +
           "AND (:status IS NULL OR :status = '' OR " +
           "     (:status = 'ACTIVE' AND j.isActive = true AND (j.expiredAt IS NULL OR j.expiredAt >= CURRENT_TIMESTAMP)) OR " +
           "     (:status = 'CLOSED' AND j.isActive = false) OR " +
           "     (:status = 'EXPIRED' AND j.expiredAt IS NOT NULL AND j.expiredAt < CURRENT_TIMESTAMP))")
    Page<Job> searchEmployerJobs(
            @Param("companyIds") Collection<Integer> companyIds,
            @Param("keyword") String keyword,
            @Param("status") String status,
            Pageable pageable
    );

    @Query("SELECT j FROM Job j WHERE j.company.id IN :companyIds " +
           "AND (CAST(:keyword AS string) IS NULL OR LOWER(j.title) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))) " +
           "AND (:status IS NULL OR :status = '' OR " +
           "     (:status = 'ACTIVE' AND j.isActive = true AND (j.expiredAt IS NULL OR j.expiredAt >= CURRENT_TIMESTAMP)) OR " +
           "     (:status = 'CLOSED' AND j.isActive = false) OR " +
           "     (:status = 'EXPIRED' AND j.expiredAt IS NOT NULL AND j.expiredAt < CURRENT_TIMESTAMP)) " +
           "ORDER BY (SELECT COUNT(a) FROM Application a WHERE a.job = j) DESC, j.id DESC")
    Page<Job> searchEmployerJobsOrderByApplications(
            @Param("companyIds") Collection<Integer> companyIds,
            @Param("keyword") String keyword,
            @Param("status") String status,
            Pageable pageable
    );
}


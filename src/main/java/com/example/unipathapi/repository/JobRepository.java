package com.example.unipathapi.repository;

import com.example.unipathapi.entity.Job;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobRepository extends JpaRepository<Job, Integer> {

    @Query("SELECT j FROM Job j WHERE j.isActive = true " +
           "AND (:cursor IS NULL OR j.id < :cursor) " +
           "AND (:keyword IS NULL OR LOWER(j.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(j.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:categoryId IS NULL OR j.category.id = :categoryId) " +
           "AND (:locationId IS NULL OR j.location.id = :locationId) " +
           "AND (:jobType IS NULL OR j.jobType = :jobType) " +
           "ORDER BY j.postedAt DESC, j.id DESC")
    List<Job> findFeedJobs(@Param("cursor") Integer cursor,
                           @Param("keyword") String keyword,
                           @Param("categoryId") Integer categoryId,
                           @Param("locationId") Integer locationId,
                           @Param("jobType") String jobType,
                           Pageable pageable);

    List<Job> findByCompanyId(Integer companyId);

    long countByIsActiveTrue();
}

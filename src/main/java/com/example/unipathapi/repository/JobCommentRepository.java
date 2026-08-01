package com.example.unipathapi.repository;

import com.example.unipathapi.entity.JobComment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobCommentRepository extends JpaRepository<JobComment, Integer> {

    @Query("SELECT c FROM JobComment c WHERE c.job.id = :jobId " +
           "AND (:cursor IS NULL OR c.id < :cursor) " +
           "ORDER BY c.id DESC")
    List<JobComment> findCommentsByJobId(@Param("jobId") Integer jobId,
                                         @Param("cursor") Integer cursor,
                                         Pageable pageable);

    long countByJobId(Integer jobId);
}

package com.example.unipathapi.repository;

import com.example.unipathapi.entity.JobLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JobLikeRepository extends JpaRepository<JobLike, Integer> {
    Optional<JobLike> findByJobIdAndUserId(Integer jobId, Integer userId);
    boolean existsByJobIdAndUserId(Integer jobId, Integer userId);
    long countByJobId(Integer jobId);
}

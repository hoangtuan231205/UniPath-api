package com.example.unipathapi.job.repository;
import com.example.unipathapi.job.entity.*;
import com.example.unipathapi.user.entity.User;

import com.example.unipathapi.job.entity.JobLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface JobLikeRepository extends JpaRepository<JobLike, Integer> {
    Optional<JobLike> findByJobIdAndUserId(Integer jobId, Integer userId);
    boolean existsByJobIdAndUserId(Integer jobId, Integer userId);
    long countByJobId(Integer jobId);

    @Query("SELECT jl.job.id FROM JobLike jl WHERE jl.user.id = :userId AND jl.job.id IN :jobIds")
    Set<Integer> findLikedJobIdsByUserIdAndJobIds(@Param("userId") Integer userId, @Param("jobIds") Collection<Integer> jobIds);

    @Query("SELECT jl.job.id, COUNT(jl) FROM JobLike jl WHERE jl.job.id IN :jobIds GROUP BY jl.job.id")
    List<Object[]> countLikesByJobIds(@Param("jobIds") Collection<Integer> jobIds);
}

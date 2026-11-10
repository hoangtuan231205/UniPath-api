package com.example.unipathapi.job.repository;

import com.example.unipathapi.job.entity.JobMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface JobMediaRepository extends JpaRepository<JobMedia, Integer> {
    List<JobMedia> findByJobIdOrderByDisplayOrderAsc(Integer jobId);
    
    long countByJobId(Integer jobId);

    @Query("SELECT m FROM JobMedia m WHERE m.job.id IN :jobIds ORDER BY m.job.id, m.displayOrder ASC")
    List<JobMedia> findByJobIdIn(@Param("jobIds") Collection<Integer> jobIds);

    @Modifying
    @Query("DELETE FROM JobMedia m WHERE m.job.id = :jobId")
    void deleteByJobId(@Param("jobId") Integer jobId);
}

package com.example.unipathapi.job.repository;
import com.example.unipathapi.job.entity.*;

import com.example.unipathapi.job.entity.JobShare;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobShareRepository extends JpaRepository<JobShare, Integer> {
    long countByJobId(Integer jobId);
}

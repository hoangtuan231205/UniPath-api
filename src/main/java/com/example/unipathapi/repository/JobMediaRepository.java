package com.example.unipathapi.repository;

import com.example.unipathapi.entity.JobMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobMediaRepository extends JpaRepository<JobMedia, Integer> {
    List<JobMedia> findByJobIdOrderByDisplayOrderAsc(Integer jobId);
    long countByJobId(Integer jobId);
}

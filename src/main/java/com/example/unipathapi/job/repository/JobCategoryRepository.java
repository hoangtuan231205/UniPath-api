package com.example.unipathapi.job.repository;
import com.example.unipathapi.job.entity.*;

import com.example.unipathapi.job.entity.JobCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JobCategoryRepository extends JpaRepository<JobCategory, Integer> {
    Optional<JobCategory> findByName(String name);
}

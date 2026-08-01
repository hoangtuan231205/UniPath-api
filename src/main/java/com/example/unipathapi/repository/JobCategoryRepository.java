package com.example.unipathapi.repository;

import com.example.unipathapi.entity.JobCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JobCategoryRepository extends JpaRepository<JobCategory, Integer> {
    Optional<JobCategory> findByName(String name);
}

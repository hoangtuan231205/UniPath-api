package com.example.unipathapi.repository;

import com.example.unipathapi.entity.CvTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CvTemplateRepository extends JpaRepository<CvTemplate, Integer> {
}

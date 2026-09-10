package com.example.unipathapi.cv.repository;
import com.example.unipathapi.cv.entity.*;

import com.example.unipathapi.cv.entity.CvTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CvTemplateRepository extends JpaRepository<CvTemplate, Integer> {
}

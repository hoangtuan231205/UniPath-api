package com.example.unipathapi.repository;

import com.example.unipathapi.entity.Employment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmploymentRepository extends JpaRepository<Employment, Integer> {
    List<Employment> findByCompanyId(Integer companyId);
    List<Employment> findByCompanyIdAndStatus(Integer companyId, String status);
}

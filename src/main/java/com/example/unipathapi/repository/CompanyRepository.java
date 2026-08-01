package com.example.unipathapi.repository;

import com.example.unipathapi.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Integer> {

    List<Company> findByCompanyNameContainingIgnoreCaseAndStatus(String keyword, String status);

    Optional<Company> findByTaxCode(String taxCode);

    List<Company> findByStatus(String status);
}

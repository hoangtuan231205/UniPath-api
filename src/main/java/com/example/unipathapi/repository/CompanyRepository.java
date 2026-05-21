package com.example.unipathapi.repository;

import com.example.unipathapi.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Integer> {
    // Kế thừa toàn bộ các hàm CRUD cơ bản (findById, save, delete,...) từ JpaRepository
}

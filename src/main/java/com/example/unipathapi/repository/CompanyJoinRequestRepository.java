package com.example.unipathapi.repository;

import com.example.unipathapi.entity.CompanyJoinRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CompanyJoinRequestRepository extends JpaRepository<CompanyJoinRequest, Integer> {

    List<CompanyJoinRequest> findByCompanyIdAndStatus(Integer companyId, String status);

    List<CompanyJoinRequest> findByUserId(Integer userId);

    List<CompanyJoinRequest> findByStatus(String status);
}

package com.example.unipathapi.repository;

import com.example.unipathapi.entity.CompanyMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyMemberRepository extends JpaRepository<CompanyMember, Integer> {

    List<CompanyMember> findByUserId(Integer userId);

    List<CompanyMember> findByCompanyId(Integer companyId);

    Optional<CompanyMember> findByCompanyIdAndUserId(Integer companyId, Integer userId);

    boolean existsByCompanyIdAndUserId(Integer companyId, Integer userId);

    boolean existsByCompanyIdAndUserIdAndMemberRole(Integer companyId, Integer userId, String memberRole);

    boolean existsByCompanyIdAndMemberRole(Integer companyId, String memberRole);
}

package com.example.unipathapi.company.repository;
import com.example.unipathapi.company.entity.*;

import com.example.unipathapi.company.entity.CompanyMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyMemberRepository extends JpaRepository<CompanyMember, Integer> {

    List<CompanyMember> findByUserId(Integer userId);

    List<CompanyMember> findByCompanyId(Integer companyId);

    Optional<CompanyMember> findByCompanyIdAndUserId(Integer companyId, Integer userId);

    boolean existsByCompanyIdAndUserId(Integer companyId, Integer userId);

    boolean existsByCompanyIdAndUserIdAndMemberRole(Integer companyId, Integer userId, String memberRole);

    boolean existsByCompanyIdAndUserIdAndMemberRoleIn(Integer companyId, Integer userId, Collection<String> memberRoles);

    boolean existsByCompanyIdAndMemberRole(Integer companyId, String memberRole);

    long countByCompanyIdAndMemberRole(Integer companyId, String memberRole);
}

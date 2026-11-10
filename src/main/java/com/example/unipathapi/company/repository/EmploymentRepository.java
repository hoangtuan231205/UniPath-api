package com.example.unipathapi.company.repository;
import com.example.unipathapi.company.entity.*;

import com.example.unipathapi.company.entity.Employment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface EmploymentRepository extends JpaRepository<Employment, Integer> {
    List<Employment> findByCompanyId(Integer companyId);
    List<Employment> findByCompanyIdAndStatus(Integer companyId, String status);
    boolean existsByCompanyIdAndCandidateIdAndStatusIn(Integer companyId, Integer candidateId, List<String> statuses);
    List<Employment> findByCompanyIdAndCandidateIdInAndStatusIn(Integer companyId, java.util.Collection<Integer> candidateIds, java.util.Collection<String> statuses);
    boolean existsByInvitationId(Integer invitationId);
    List<Employment> findByStatusAndStartDateLessThanEqual(String status, LocalDate date);
    List<Employment> findByCandidateIdAndStatusIn(Integer candidateId, List<String> statuses);
}

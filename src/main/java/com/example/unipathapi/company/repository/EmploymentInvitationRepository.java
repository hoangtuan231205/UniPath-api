package com.example.unipathapi.company.repository;

import com.example.unipathapi.company.entity.EmploymentInvitation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmploymentInvitationRepository extends JpaRepository<EmploymentInvitation, Integer> {

    boolean existsByCompanyIdAndCandidateIdAndStatus(Integer companyId, Integer candidateId, String status);

    boolean existsByApplicationId(Integer applicationId);

    boolean existsByApplicationIdAndStatusIn(Integer applicationId, java.util.Collection<String> statuses);

    Optional<EmploymentInvitation> findByApplicationId(Integer applicationId);

    Optional<EmploymentInvitation> findTopByApplicationIdOrderByIdDesc(Integer applicationId);

    List<EmploymentInvitation> findByApplicationIdIn(java.util.Collection<Integer> applicationIds);

    Page<EmploymentInvitation> findByCompanyId(Integer companyId, Pageable pageable);

    Page<EmploymentInvitation> findByCompanyIdAndStatus(Integer companyId, String status, Pageable pageable);

    List<EmploymentInvitation> findByCandidateId(Integer candidateId);

    Page<EmploymentInvitation> findByCandidateId(Integer candidateId, Pageable pageable);

    List<EmploymentInvitation> findByCandidateIdAndStatus(Integer candidateId, String status);

    Page<EmploymentInvitation> findByCandidateIdAndStatus(Integer candidateId, String status, Pageable pageable);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT i FROM EmploymentInvitation i WHERE i.id = :id")
    Optional<EmploymentInvitation> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") Integer id);

    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE EmploymentInvitation e SET e.status = 'ACCEPTED', e.respondedAt = :now WHERE e.id = :id AND e.candidate.id = :candidateId AND e.status = 'PENDING' AND (e.expiresAt IS NULL OR e.expiresAt > :now)")
    int acceptAtomic(@org.springframework.data.repository.query.Param("id") Integer id, @org.springframework.data.repository.query.Param("candidateId") Integer candidateId, @org.springframework.data.repository.query.Param("now") java.time.LocalDateTime now);

    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE EmploymentInvitation e SET e.status = 'DECLINED', e.declineReason = :reason, e.respondedAt = :now WHERE e.id = :id AND e.candidate.id = :candidateId AND e.status = 'PENDING' AND (e.expiresAt IS NULL OR e.expiresAt > :now)")
    int declineAtomic(@org.springframework.data.repository.query.Param("id") Integer id, @org.springframework.data.repository.query.Param("candidateId") Integer candidateId, @org.springframework.data.repository.query.Param("reason") String reason, @org.springframework.data.repository.query.Param("now") java.time.LocalDateTime now);

    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE EmploymentInvitation e SET e.status = 'CANCELLED', e.respondedAt = :now WHERE e.id = :id AND e.company.id = :companyId AND e.status = 'PENDING'")
    int cancelAtomic(@org.springframework.data.repository.query.Param("id") Integer id, @org.springframework.data.repository.query.Param("companyId") Integer companyId, @org.springframework.data.repository.query.Param("now") java.time.LocalDateTime now);

    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE EmploymentInvitation e SET e.status = 'EXPIRED' WHERE e.id = :id AND e.status = 'PENDING' AND e.expiresAt IS NOT NULL AND e.expiresAt <= :now")
    int expireAtomic(@org.springframework.data.repository.query.Param("id") Integer id, @org.springframework.data.repository.query.Param("now") java.time.LocalDateTime now);
}

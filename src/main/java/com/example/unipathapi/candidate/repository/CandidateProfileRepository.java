package com.example.unipathapi.candidate.repository;

import com.example.unipathapi.candidate.entity.CandidateProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CandidateProfileRepository extends JpaRepository<CandidateProfile, Integer> {
    Optional<CandidateProfile> findByUserId(Integer userId);

    @Query("SELECT cp FROM CandidateProfile cp JOIN cp.user u " +
           "WHERE (u.isActive IS NULL OR u.isActive = true) " +
           "AND (u.role IS NULL OR UPPER(u.role) = 'CANDIDATE') " +
           "AND LOWER(TRIM(u.email)) = LOWER(TRIM(:email))")
    Page<CandidateProfile> findByExactEmail(@Param("email") String email, Pageable pageable);

    @Query("SELECT cp FROM CandidateProfile cp JOIN cp.user u " +
           "WHERE (u.isActive IS NULL OR u.isActive = true) " +
           "AND (u.role IS NULL OR UPPER(u.role) = 'CANDIDATE') " +
           "AND (:skill IS NULL OR :skill = '' OR LOWER(cp.skills) LIKE LOWER(CONCAT('%', :skill, '%'))) " +
           "AND (:major IS NULL OR :major = '' OR LOWER(cp.major) LIKE LOWER(CONCAT('%', :major, '%'))) " +
           "AND (:university IS NULL OR :university = '' OR LOWER(cp.universityName) LIKE LOWER(CONCAT('%', :university, '%'))) " +
           "AND (:keyword IS NULL OR :keyword = '' OR (" +
           "     LOWER(cp.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "     OR LOWER(cp.major) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "     OR LOWER(cp.universityName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "     OR LOWER(cp.skills) LIKE LOWER(CONCAT('%', :keyword, '%'))))")
    Page<CandidateProfile> searchProfiles(@Param("skill") String skill,
                                          @Param("major") String major,
                                          @Param("university") String university,
                                          @Param("keyword") String keyword,
                                          Pageable pageable);
}

package com.example.unipathapi.candidate.repository;

import com.example.unipathapi.candidate.entity.CandidateProfile;
import com.example.unipathapi.candidate.entity.CandidateProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CandidateProfileRepository extends JpaRepository<CandidateProfile, Integer> {
    Optional<CandidateProfile> findByUserId(Integer userId);
}

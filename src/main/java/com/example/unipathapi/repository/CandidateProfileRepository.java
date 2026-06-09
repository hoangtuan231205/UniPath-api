package com.example.unipathapi.repository;
import com.example.unipathapi.entity.CandidateProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CandidateProfileRepository extends JpaRepository<CandidateProfile, Integer> {
    Optional<CandidateProfile> findByUserId(Integer userId);
}

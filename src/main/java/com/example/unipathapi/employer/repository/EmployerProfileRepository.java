package com.example.unipathapi.employer.repository;

import com.example.unipathapi.employer.entity.EmployerProfile;
import com.example.unipathapi.candidate.entity.CandidateProfile;
import com.example.unipathapi.employer.entity.EmployerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
public interface EmployerProfileRepository extends JpaRepository<EmployerProfile, Integer> {
    Optional<EmployerProfile> findByUserId(Integer userid);
}

package com.example.unipathapi.repository;
import com.example.unipathapi.entity.CandidateProfile;
import com.example.unipathapi.entity.EmployerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
public interface EmployerProfileRepository extends JpaRepository<EmployerProfile, Integer> {
    Optional<EmployerProfile> findByUserId(Integer userid);
}

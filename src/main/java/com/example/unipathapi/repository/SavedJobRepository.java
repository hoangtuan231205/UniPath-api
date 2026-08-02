package com.example.unipathapi.repository;

import com.example.unipathapi.entity.SavedJob;
import com.example.unipathapi.entity.SavedJobId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SavedJobRepository extends JpaRepository<SavedJob, SavedJobId> {
    List<SavedJob> findByCandidateIdOrderBySavedAtDesc(Integer candidateId);
    boolean existsByCandidateIdAndJobId(Integer candidateId, Integer jobId);
    void deleteByCandidateIdAndJobId(Integer candidateId, Integer jobId);
}

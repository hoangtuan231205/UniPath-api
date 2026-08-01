package com.example.unipathapi.repository;

import com.example.unipathapi.entity.JobSkill;
import com.example.unipathapi.entity.JobSkillId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobSkillRepository extends JpaRepository<JobSkill, JobSkillId> {
    List<JobSkill> findByJobId(Integer jobId);
    void deleteByJobId(Integer jobId);
    boolean existsBySkillId(Integer skillId);
}

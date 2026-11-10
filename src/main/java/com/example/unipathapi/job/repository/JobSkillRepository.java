package com.example.unipathapi.job.repository;
import com.example.unipathapi.job.entity.*;

import com.example.unipathapi.job.entity.JobSkill;
import com.example.unipathapi.job.entity.JobSkillId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobSkillRepository extends JpaRepository<JobSkill, JobSkillId> {
    List<JobSkill> findByJobId(Integer jobId);
    void deleteByJobId(Integer jobId);
    boolean existsBySkillId(Integer skillId);

    @org.springframework.data.jpa.repository.Query("SELECT js FROM JobSkill js JOIN FETCH js.skill WHERE js.job.id IN :jobIds")
    List<JobSkill> findByJobIdIn(@org.springframework.data.repository.query.Param("jobIds") java.util.Collection<Integer> jobIds);
}

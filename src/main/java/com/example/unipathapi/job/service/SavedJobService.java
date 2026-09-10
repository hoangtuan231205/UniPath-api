package com.example.unipathapi.job.service;
import com.example.unipathapi.job.entity.*;
import com.example.unipathapi.job.repository.*;
import com.example.unipathapi.job.dto.request.*;
import com.example.unipathapi.job.dto.response.*;
import com.example.unipathapi.job.service.*;

import com.example.unipathapi.job.dto.response.JobResponse;
import com.example.unipathapi.job.entity.Job;
import com.example.unipathapi.job.entity.SavedJob;
import com.example.unipathapi.user.entity.User;
import com.example.unipathapi.job.repository.JobRepository;
import com.example.unipathapi.job.repository.SavedJobRepository;
import com.example.unipathapi.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SavedJobService {

    @Autowired
    private SavedJobRepository savedJobRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JobService jobService;

    public void saveJob(Integer jobId, Integer candidateId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tin tuyển dụng"));
        User candidate = userRepository.findById(candidateId)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        if (!savedJobRepository.existsByCandidateIdAndJobId(candidateId, jobId)) {
            SavedJob savedJob = new SavedJob(candidate, job);
            savedJobRepository.save(savedJob);
        }
    }

    @Transactional
    public void unsaveJob(Integer jobId, Integer candidateId) {
        savedJobRepository.deleteByCandidateIdAndJobId(candidateId, jobId);
    }

    public List<JobResponse> getSavedJobs(Integer candidateId) {
        List<SavedJob> savedJobs = savedJobRepository.findByCandidateIdOrderBySavedAtDesc(candidateId);
        return savedJobs.stream()
                .map(sj -> jobService.buildJobResponse(sj.getJob()))
                .collect(Collectors.toList());
    }
}

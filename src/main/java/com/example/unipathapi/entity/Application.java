package com.example.unipathapi.entity;
import com.example.unipathapi.job.entity.*;
import com.example.unipathapi.job.repository.*;
import com.example.unipathapi.job.dto.request.*;
import com.example.unipathapi.job.dto.response.*;
import com.example.unipathapi.job.service.*;
import com.example.unipathapi.user.entity.User;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "applications")
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id")
    private Job job;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id")
    private User candidate;

    @Column(name = "cv_profile_id")
    private Integer cvProfileId;

    @Column(name = "cv_url", columnDefinition = "TEXT")
    private String cvUrl;

    @Column(name = "cv_filename", length = 255)
    private String cvFilename;

    @Column(name = "cv_file_type", length = 10)
    private String cvFileType;

    @Column(name = "cv_uploaded_at", insertable = false, updatable = false)
    private LocalDateTime cvUploadedAt;

    @Column(name = "cover_letter", columnDefinition = "TEXT")
    private String coverLetter;

    @Column(length = 50)
    private String status = "SUBMITTED";

    @Column(name = "applied_at", insertable = false, updatable = false)
    private LocalDateTime appliedAt;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "interview_at")
    private LocalDateTime interviewAt;

    @Column(name = "interview_location", length = 255)
    private String interviewLocation;
}

package com.example.unipathapi.entity;

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
    @JoinColumn(name = "student_id")
    private User student;

    @Column(name = "cv_url", columnDefinition = "TEXT")
    private String cvUrl;

    @Column(name = "cv_filename", length = 255)
    private String cvFilename;

    @Column(name = "cv_file_type", length = 10)
    private String cvFileType;

    @Column(name = "cv_uploaded_at", insertable = false, updatable = false)
    private LocalDateTime cvUploadedAt;

    @Column(length = 50)
    private String status = "SUBMITTED";

    @Column(name = "applied_at", insertable = false, updatable = false)
    private LocalDateTime appliedAt;
}

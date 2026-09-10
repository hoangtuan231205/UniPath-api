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
@Table(name = "reports")
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id")
    private User reporter;

    @Column(name = "target_type", length = 10, nullable = false)
    private String targetType = "JOB"; // 'JOB', 'POST'

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id")
    private Job job;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private CommunityPost post;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(length = 50)
    private String status = "PENDING"; // 'PENDING', 'RESOLVED', 'REJECTED'

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by")
    private User resolvedBy;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}

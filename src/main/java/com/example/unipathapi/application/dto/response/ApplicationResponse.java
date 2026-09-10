package com.example.unipathapi.application.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class ApplicationResponse {
    private Integer id;
    private Integer jobId;
    private String jobTitle;
    private String companyName;

    private Integer candidateId;
    private String candidateName;
    private String candidateEmail;

    private Integer cvProfileId;
    private String cvUrl;
    private String cvFilename;
    private String cvFileType;
    private LocalDateTime cvUploadedAt;

    private String coverLetter;
    private String status;
    private LocalDateTime appliedAt;

    private String note;
    private LocalDateTime interviewAt;
    private String interviewLocation;
}

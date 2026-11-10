package com.example.unipathapi.application.dto.response;

import com.example.unipathapi.job.enums.JobType;
import com.example.unipathapi.job.enums.WorkMode;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApplicationResponse {
    private Integer id;
    private Integer jobId;
    private String jobTitle;

    private Integer companyId;
    private String companyName;
    private String companyLogoUrl;
    private String locationAddress;
    private JobType jobType;
    private WorkMode workMode;
    private String salaryRange;

    private Integer candidateId;
    private String candidateName;
    private String candidateEmail;

    private Integer cvProfileId;
    private String cvFilename;
    private String cvFileType;
    private LocalDateTime cvUploadedAt;

    private String coverLetter;
    private String status;
    private LocalDateTime appliedAt;

    private String note;
    private LocalDateTime interviewAt;
    private String interviewLocation;

    private Boolean canWithdraw;

    private Integer invitationId;
    private String invitationStatus;
    private LocalDateTime invitationSentAt;
    private Boolean hasActiveEmployment;
}

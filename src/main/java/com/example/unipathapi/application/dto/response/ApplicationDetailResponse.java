package com.example.unipathapi.application.dto.response;

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
public class ApplicationDetailResponse {
    private Integer id;
    private Integer jobId;
    private String jobTitle;

    private Integer companyId;
    private String companyName;

    private String status;
    private LocalDateTime appliedAt;

    private String coverLetter;
    private String note;
    private LocalDateTime interviewAt;
    private String interviewLocation;

    private CandidateInfo candidate;
    private CvInfo cv;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CandidateInfo {
        private Integer id;
        private String email;
        private String fullName;
        private String phoneNumber;
        private String universityName;
        private String major;
        private String skills;
        private Integer experienceYears;
        private String avatarUrl;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CvInfo {
        private Boolean hasCvFile;
        private String cvFilename;
        private String cvFileType;
        private LocalDateTime cvUploadedAt;

        private Boolean hasCvOnline;
        private Integer cvProfileId;
        private String cvSnapshotJson;
        private Boolean isCvOnlineAvailable;
        private String cvOnlineStatusMessage;
    }
}

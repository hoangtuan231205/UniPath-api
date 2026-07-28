package com.example.unipathapi.dto.response;

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

    private Integer studentId;
    private String studentName;
    private String studentEmail;

    private String cvUrl;
    private String cvFilename;
    private String cvFileType;
    private LocalDateTime cvUploadedAt;

    private String status;
    private LocalDateTime appliedAt;
}

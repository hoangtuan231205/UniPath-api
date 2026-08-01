package com.example.unipathapi.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class ReportResponse {
    private Integer id;
    private Integer reporterId;
    private String reporterEmail;

    private Integer jobId;
    private String jobTitle;

    private String reason;
    private String status;

    private Integer resolvedById;
    private String resolvedByName;
    private LocalDateTime resolvedAt;

    private LocalDateTime createdAt;
}

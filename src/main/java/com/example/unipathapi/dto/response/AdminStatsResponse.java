package com.example.unipathapi.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@Builder
public class AdminStatsResponse {
    private long totalCandidates;
    private long totalEmployers;
    private long activeJobs;
    private Map<String, Long> applicationStatusCounts;
}

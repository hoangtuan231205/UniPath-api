package com.example.unipathapi.job.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployerJobStatsResponse {
    private long totalJobs;
    private long activeJobs;
    private long closedJobs;
    private long totalApplications;
}

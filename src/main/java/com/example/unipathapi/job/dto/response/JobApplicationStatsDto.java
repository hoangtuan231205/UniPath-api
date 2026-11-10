package com.example.unipathapi.job.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class JobApplicationStatsDto {
    private Integer jobId;
    private long totalCount;
    private long newCount;
    private long interviewCount;
}

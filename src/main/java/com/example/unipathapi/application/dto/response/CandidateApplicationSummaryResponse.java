package com.example.unipathapi.application.dto.response;

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
public class CandidateApplicationSummaryResponse {
    private long total;
    private long submitted;
    private long reviewing;
    private long interview;
    private long accepted;
    private long rejected;
    private long withdrawn;
}

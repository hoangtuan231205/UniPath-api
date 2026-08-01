package com.example.unipathapi.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class CandidateSearchResponse {
    private Integer userId;
    private String fullName;
    private String universityName;
    private String major;
    private Integer experienceYears;
    private String phoneNumber;
    private String skills;
}

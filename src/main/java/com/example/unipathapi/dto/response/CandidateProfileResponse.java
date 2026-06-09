package com.example.unipathapi.dto.response;

import lombok.Data;

@Data

public class CandidateProfileResponse {
    private Integer id;
    private String email;
    private String fullName;
    private String phoneNumber;
    private Integer experienceYears;
    private String universityName;
    private String major;
    private String skills;
}

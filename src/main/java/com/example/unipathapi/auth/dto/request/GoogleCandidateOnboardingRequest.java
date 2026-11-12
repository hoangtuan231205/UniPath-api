package com.example.unipathapi.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request DTO for completing Google Candidate Onboarding.
 * Email and Google identity are securely derived from the onboarding session.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoogleCandidateOnboardingRequest {

    @NotBlank(message = "Onboarding token không được để trống")
    private String onboardingToken;

    private String fullName;
    private String phoneNumber;
    private String universityName;
    private String major;
    private Integer experienceYears;
    private String skills;
}


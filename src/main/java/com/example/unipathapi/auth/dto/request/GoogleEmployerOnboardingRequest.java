package com.example.unipathapi.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request DTO for completing Google Employer Onboarding.
 * Email and Google identity are securely derived from the onboarding session.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoogleEmployerOnboardingRequest {

    @NotBlank(message = "Onboarding token không được để trống")
    private String onboardingToken;

    private String fullName;
    private String phone;
    private String position;
    private String bio;

    // Company selection: exactly one of companyId or newCompanyName must be provided
    private Integer companyId;

    // In case of proposing a new company
    private String newCompanyName;
    private String newCompanyContactEmail;
    private String newCompanyTaxCode;
    private String newCompanyScale;
    private String newCompanyWebsite;
    private String newCompanyPhoneNumber;
    private String newCompanyDescription;

    @Builder.Default
    private Boolean confirmed = false;
}


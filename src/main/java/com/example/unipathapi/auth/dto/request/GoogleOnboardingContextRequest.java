package com.example.unipathapi.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request DTO for retrieving verified Google Onboarding context on the registration form.
 * Uses POST body so the opaque onboarding token is never exposed in URL query strings or access logs.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GoogleOnboardingContextRequest {

    @NotBlank(message = "Onboarding token không được để trống")
    private String onboardingToken;
}

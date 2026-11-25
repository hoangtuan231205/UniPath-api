package com.example.unipathapi.auth.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request DTO for Google OAuth 2.0 Authorization Code exchange (Phase 3.5).
 *
 * Security Rules:
 * - Requires authorization code issued by Google Identity Services (GIS) Popup UX.
 * - Supports alias "code" and "authCode" for client flexibility.
 * - Client NEVER sends user details, sub, or email. Identity is derived strictly from verified Google ID token.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoogleAuthRequest {

    @NotBlank(message = "Authorization code không được để trống")
    @JsonAlias({"code", "authCode"})
    private String authorizationCode;
}


package com.example.unipathapi.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request DTO for linking an existing UniPath account with a verified Google identity.
 *
 * Security Rules:
 * - Requires the raw link token issued by GoogleAuthService during ACCOUNT_LINK_REQUIRED flow.
 * - Requires the current UniPath account password to prove account ownership (prevents Account Takeover).
 * - Client NEVER sends userId or googleSub directly. Identity is derived strictly from the verified session.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoogleAccountLinkRequest {

    @NotBlank(message = "Link token không được để trống")
    private String linkToken;

    @NotBlank(message = "Mật khẩu không được để trống")
    private String password;
}


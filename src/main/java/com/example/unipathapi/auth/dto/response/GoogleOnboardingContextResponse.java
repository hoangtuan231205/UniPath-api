package com.example.unipathapi.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Verified Google identity context bound to an active GoogleOnboardingSession.
 * Exposes only safe display fields needed to prefill the registration form;
 * never exposes token hashes or internal Google subject identifiers (sub).
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoogleOnboardingContextResponse {

    private String email;
    private String name;
    private String picture;
    private LocalDateTime expiresAt;
}

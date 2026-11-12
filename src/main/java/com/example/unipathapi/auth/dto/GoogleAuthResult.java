package com.example.unipathapi.auth.dto;

import com.example.unipathapi.auth.dto.response.AuthResponse;

import java.time.LocalDateTime;

/**
 * Result representing the outcome of a verified Google Authentication process in UniPath.
 *
 * Sealed interface with three distinct outcomes:
 * 1. Authenticated: Google identity is already linked to a UniPath User -> returns valid JWT & user info.
 * 2. OnboardingRequired: Google identity is verified, but user does not exist in UniPath -> returns raw onboarding token.
 * 3. AccountLinkRequired: Google identity is verified, but email matches an existing password-based account -> returns raw link token.
 */
public sealed interface GoogleAuthResult permits
        GoogleAuthResult.Authenticated,
        GoogleAuthResult.OnboardingRequired,
        GoogleAuthResult.AccountLinkRequired {

    GoogleAuthStatus getStatus();

    record Authenticated(
            AuthResponse authResponse,
            Integer userId,
            String email,
            String role
    ) implements GoogleAuthResult {
        @Override
        public GoogleAuthStatus getStatus() {
            return GoogleAuthStatus.AUTHENTICATED;
        }
    }

    record OnboardingRequired(
            String onboardingToken,
            String email,
            String name,
            String picture,
            LocalDateTime expiresAt
    ) implements GoogleAuthResult {
        @Override
        public GoogleAuthStatus getStatus() {
            return GoogleAuthStatus.ONBOARDING_REQUIRED;
        }
    }

    record AccountLinkRequired(
            String linkToken,
            String email,
            LocalDateTime expiresAt
    ) implements GoogleAuthResult {
        @Override
        public GoogleAuthStatus getStatus() {
            return GoogleAuthStatus.ACCOUNT_LINK_REQUIRED;
        }
    }

    static Authenticated authenticated(AuthResponse authResponse, Integer userId, String email, String role) {
        return new Authenticated(authResponse, userId, email, role);
    }

    static OnboardingRequired onboardingRequired(String onboardingToken, String email, String name, String picture, LocalDateTime expiresAt) {
        return new OnboardingRequired(onboardingToken, email, name, picture, expiresAt);
    }

    static AccountLinkRequired accountLinkRequired(String linkToken, String email, LocalDateTime expiresAt) {
        return new AccountLinkRequired(linkToken, email, expiresAt);
    }
}


package com.example.unipathapi.auth.controller;

import com.example.unipathapi.auth.dto.GoogleAuthResult;
import com.example.unipathapi.auth.dto.request.GoogleAccountLinkRequest;
import com.example.unipathapi.auth.dto.request.GoogleAuthRequest;
import com.example.unipathapi.auth.dto.request.GoogleCandidateOnboardingRequest;
import com.example.unipathapi.auth.dto.request.GoogleEmployerOnboardingRequest;
import com.example.unipathapi.auth.dto.request.GoogleOnboardingContextRequest;
import com.example.unipathapi.auth.dto.response.AuthResponse;
import com.example.unipathapi.auth.dto.response.GoogleOnboardingContextResponse;
import com.example.unipathapi.auth.service.GoogleAccountLinkService;
import com.example.unipathapi.auth.service.GoogleAuthService;
import com.example.unipathapi.auth.service.GoogleOnboardingService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller providing API endpoints for Google Authentication, Account Linking,
 * and New Account Onboarding (Phase 3.4.1, 3.5 & Google Onboarding Completion).
 *
 * Rules:
 * - POST /api/auth/google: Exchanges Google Authorization Code and processes identity.
 * - POST /api/auth/google/link: Links an existing UniPath account with a verified Google identity.
 * - POST /api/auth/google/onboarding/context: Retrieves verified Google profile bound to an active onboarding token.
 * - POST /api/auth/google/onboarding/candidate: Completes Candidate signup with verified Google onboarding session.
 * - POST /api/auth/google/onboarding/employer: Completes Employer signup with verified Google onboarding session.
 * - Accepts only opaque tokens and profile fields. Never accepts userId, googleSub, or email directly from client.
 * - All validation and business exceptions are handled centrally via GlobalExceptionHandler.
 */
@RestController
@RequestMapping("/api/auth/google")
public class GoogleAccountLinkController {

    private final GoogleAccountLinkService accountLinkService;
    private final GoogleAuthService googleAuthService;
    private final com.example.unipathapi.auth.service.GoogleOAuthSecurityValidator securityValidator;
    private final GoogleOnboardingService googleOnboardingService;

    public GoogleAccountLinkController(GoogleAccountLinkService accountLinkService) {
        this(accountLinkService, null, null, null);
    }

    public GoogleAccountLinkController(
            GoogleAccountLinkService accountLinkService,
            GoogleAuthService googleAuthService,
            com.example.unipathapi.auth.service.GoogleOAuthSecurityValidator securityValidator) {
        this(accountLinkService, googleAuthService, securityValidator, null);
    }

    @Autowired
    public GoogleAccountLinkController(
            GoogleAccountLinkService accountLinkService,
            @Autowired(required = false) GoogleAuthService googleAuthService,
            @Autowired(required = false) com.example.unipathapi.auth.service.GoogleOAuthSecurityValidator securityValidator,
            @Autowired(required = false) GoogleOnboardingService googleOnboardingService) {
        this.accountLinkService = accountLinkService;
        this.googleAuthService = googleAuthService;
        this.securityValidator = securityValidator;
        this.googleOnboardingService = googleOnboardingService;
    }

    /**
     * POST /api/auth/google
     * Exchanges Google Authorization Code and verifies Google identity (Phase 3.5).
     *
     * @param request payload containing authorizationCode
     * @param origin Origin header from browser
     * @param requestedWith X-Requested-With header from GIS Popup flow
     * @return ResponseEntity containing GoogleAuthResult (AUTHENTICATED, ACCOUNT_LINK_REQUIRED, or ONBOARDING_REQUIRED)
     */
    @PostMapping({"", "/"})
    public ResponseEntity<GoogleAuthResult> authenticateWithGoogle(
            @Valid @RequestBody GoogleAuthRequest request,
            @RequestHeader(value = "Origin", required = false) String origin,
            @RequestHeader(value = "X-Requested-With", required = false) String requestedWith) {
        if (securityValidator != null && origin != null) {
            securityValidator.validatePopupHeaders(origin, requestedWith);
        }
        if (googleAuthService == null) {
            throw new IllegalStateException("GoogleAuthService is not configured");
        }
        GoogleAuthResult result = googleAuthService.authenticateWithGoogleCode(request.getAuthorizationCode());
        return ResponseEntity.ok(result);
    }

    /**
     * POST /api/auth/google/link
     * Links an existing UniPath account with a verified Google identity.
     *
     * @param request payload containing raw linkToken and UniPath account password
     * @return ResponseEntity containing AuthResponse with newly generated JWT
     */
    @PostMapping("/link")
    public ResponseEntity<AuthResponse> linkAccount(@Valid @RequestBody GoogleAccountLinkRequest request) {
        AuthResponse response = accountLinkService.linkAccount(request);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/auth/google/onboarding/context
     * Validates an active onboarding token and returns the server-verified Google identity for form prefill.
     */
    @PostMapping("/onboarding/context")
    public ResponseEntity<GoogleOnboardingContextResponse> getOnboardingContext(
            @Valid @RequestBody GoogleOnboardingContextRequest request,
            @RequestHeader(value = "Origin", required = false) String origin,
            @RequestHeader(value = "X-Requested-With", required = false) String requestedWith) {
        if (securityValidator != null && origin != null) {
            securityValidator.validatePopupHeaders(origin, requestedWith);
        }
        if (googleOnboardingService == null) {
            throw new IllegalStateException("GoogleOnboardingService is not configured");
        }
        GoogleOnboardingContextResponse context = googleOnboardingService.getOnboardingContext(request.getOnboardingToken());
        return ResponseEntity.ok(context);
    }

    /**
     * POST /api/auth/google/onboarding/candidate
     * Atomically consumes the onboarding token, creates Candidate User + Profile, links Google identity, and issues JWT.
     */
    @PostMapping("/onboarding/candidate")
    public ResponseEntity<AuthResponse> completeCandidateOnboarding(
            @Valid @RequestBody GoogleCandidateOnboardingRequest request,
            @RequestHeader(value = "Origin", required = false) String origin,
            @RequestHeader(value = "X-Requested-With", required = false) String requestedWith) {
        if (securityValidator != null && origin != null) {
            securityValidator.validatePopupHeaders(origin, requestedWith);
        }
        if (googleOnboardingService == null) {
            throw new IllegalStateException("GoogleOnboardingService is not configured");
        }
        AuthResponse response = googleOnboardingService.completeCandidateOnboarding(request);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/auth/google/onboarding/employer
     * Atomically consumes the onboarding token, creates Employer User + Profile + Company association, links Google identity, and issues JWT.
     */
    @PostMapping("/onboarding/employer")
    public ResponseEntity<AuthResponse> completeEmployerOnboarding(
            @Valid @RequestBody GoogleEmployerOnboardingRequest request,
            @RequestHeader(value = "Origin", required = false) String origin,
            @RequestHeader(value = "X-Requested-With", required = false) String requestedWith) {
        if (securityValidator != null && origin != null) {
            securityValidator.validatePopupHeaders(origin, requestedWith);
        }
        if (googleOnboardingService == null) {
            throw new IllegalStateException("GoogleOnboardingService is not configured");
        }
        AuthResponse response = googleOnboardingService.completeEmployerOnboarding(request);
        return ResponseEntity.ok(response);
    }
}


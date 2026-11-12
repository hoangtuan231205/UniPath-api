package com.example.unipathapi.auth.service;

import com.example.unipathapi.auth.dto.GoogleAuthResult;
import com.example.unipathapi.auth.dto.GoogleTokenPayload;
import com.example.unipathapi.auth.dto.response.AuthResponse;
import com.example.unipathapi.auth.entity.GoogleAccountLinkSession;
import com.example.unipathapi.auth.entity.GoogleOnboardingSession;
import com.example.unipathapi.auth.entity.UserAuthProvider;
import com.example.unipathapi.auth.exception.GoogleAuthException;
import com.example.unipathapi.auth.exception.InvalidGoogleTokenException;
import com.example.unipathapi.auth.repository.GoogleAccountLinkSessionRepository;
import com.example.unipathapi.auth.repository.GoogleOnboardingSessionRepository;
import com.example.unipathapi.auth.repository.UserAuthProviderRepository;
import com.example.unipathapi.common.security.JwtUtil;
import com.example.unipathapi.user.entity.User;
import com.example.unipathapi.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Service orchestrating Google Authentication business logic for UniPath.
 *
 * Rules:
 * - Flow A: Identity already linked (provider=GOOGLE, provider_user_id=sub) -> returns Authenticated result with standard UniPath JWT.
 * - Flow B1: Email exists in UniPath User table but not linked to Google -> returns AccountLinkRequired result (no auto-linking).
 * - Flow B2: Email does not exist in UniPath -> creates GoogleOnboardingSession with hashed token and returns OnboardingRequired result.
 * - Raw tokens are never logged or stored in the database.
 * - Identity must always come from verified GoogleTokenPayload.
 */
@Service
public class GoogleAuthService {

    private static final Logger log = LoggerFactory.getLogger(GoogleAuthService.class);

    public static final String PROVIDER_GOOGLE = "GOOGLE";
    public static final Duration ONBOARDING_SESSION_TTL = Duration.ofMinutes(15);
    public static final Duration LINK_SESSION_TTL = Duration.ofMinutes(15);

    private final GoogleTokenVerifierService tokenVerifierService;
    private final SecureTokenService secureTokenService;
    private final UserAuthProviderRepository userAuthProviderRepository;
    private final GoogleOnboardingSessionRepository onboardingSessionRepository;
    private final GoogleAccountLinkSessionRepository accountLinkSessionRepository;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    public GoogleAuthService(
            GoogleTokenVerifierService tokenVerifierService,
            SecureTokenService secureTokenService,
            UserAuthProviderRepository userAuthProviderRepository,
            GoogleOnboardingSessionRepository onboardingSessionRepository,
            GoogleAccountLinkSessionRepository accountLinkSessionRepository,
            UserRepository userRepository,
            JwtUtil jwtUtil) {
        this.tokenVerifierService = tokenVerifierService;
        this.secureTokenService = secureTokenService;
        this.userAuthProviderRepository = userAuthProviderRepository;
        this.onboardingSessionRepository = onboardingSessionRepository;
        this.accountLinkSessionRepository = accountLinkSessionRepository;
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
    }

    /**
     * Authenticates a user by exchanging Google Authorization Code and processing verified identity.
     *
     * @param authorizationCode OAuth authorization code from Google GIS Popup
     * @return GoogleAuthResult (AUTHENTICATED, ONBOARDING_REQUIRED, or ACCOUNT_LINK_REQUIRED)
     */
    public GoogleAuthResult authenticateWithGoogleCode(String authorizationCode) {
        GoogleTokenPayload payload = tokenVerifierService.exchangeCodeAndVerify(authorizationCode);
        return processGoogleIdentity(payload);
    }

    /**
     * Authenticates a user by verifying a raw Google ID Token JWT.
     *
     * @param idToken raw Google ID Token JWT
     * @return GoogleAuthResult
     */
    public GoogleAuthResult authenticateWithIdToken(String idToken) {
        GoogleTokenPayload payload = tokenVerifierService.verifyIdToken(idToken);
        return processGoogleIdentity(payload);
    }

    /**
     * Core business logic executing transactional identity resolution.
     *
     * @param payload verified GoogleTokenPayload
     * @return GoogleAuthResult
     */
    @Transactional
    public GoogleAuthResult processGoogleIdentity(GoogleTokenPayload payload) {
        if (payload == null || payload.getSub() == null || payload.getSub().isBlank()) {
            throw new InvalidGoogleTokenException("Google ID Token missing subject (sub)");
        }
        if (payload.getEmail() == null || payload.getEmail().isBlank()) {
            throw new InvalidGoogleTokenException("Google ID Token missing email");
        }
        if (!payload.isEmailVerified()) {
            throw new InvalidGoogleTokenException("Google email is not verified");
        }

        String googleSub = payload.getSub().trim();
        String normalizedEmail = payload.getEmail().trim().toLowerCase();

        log.info("Processing Google identity for sub: {}, email: {}", maskSub(googleSub), maskEmail(normalizedEmail));

        // 1. Check if Google sub is already linked in UserAuthProvider
        Optional<UserAuthProvider> providerOpt = userAuthProviderRepository.findByProviderAndProviderUserId(PROVIDER_GOOGLE, googleSub);

        if (providerOpt.isPresent()) {
            // === LUỒNG A: Người dùng đã liên kết Google ===
            return handleLinkedUserLogin(providerOpt.get());
        }

        // === LUỒNG B: Google User chưa có liên kết trong UniPath ===
        return handleUnlinkedGoogleUser(payload, googleSub, normalizedEmail);
    }

    private GoogleAuthResult handleLinkedUserLogin(UserAuthProvider provider) {
        User user = provider.getUser();
        if (user == null || user.getId() == null) {
            log.error("Google provider linked to null or unpersisted User");
            throw new GoogleAuthException("Tài khoản người dùng liên kết không tồn tại");
        }

        // Fetch managed user from DB to guarantee freshest state
        User managedUser = userRepository.findById(user.getId()).orElse(null);
        if (managedUser == null) {
            log.error("Linked User ID {} not found in database", user.getId());
            throw new GoogleAuthException("Tài khoản người dùng liên kết không tồn tại");
        }

        // Verify account active status
        if (Boolean.FALSE.equals(managedUser.getIsActive())) {
            log.warn("Blocked login attempt for deactivated User ID: {}", managedUser.getId());
            throw new GoogleAuthException("Tài khoản của bạn đã bị khoá. Vui lòng liên hệ quản trị viên.");
        }

        // Generate standard UniPath JWT token
        String token = jwtUtil.generateToken(managedUser.getId(), managedUser.getRole());
        AuthResponse authResponse = new AuthResponse(
                token,
                String.valueOf(managedUser.getId()),
                managedUser.getRole(),
                "Đăng nhập bằng Google thành công!"
        );

        log.info("User ID: {} successfully authenticated via Google OAuth", managedUser.getId());
        return GoogleAuthResult.authenticated(authResponse, managedUser.getId(), managedUser.getEmail(), managedUser.getRole());
    }

    private GoogleAuthResult handleUnlinkedGoogleUser(GoogleTokenPayload payload, String googleSub, String normalizedEmail) {
        Optional<User> existingUserOpt = userRepository.findByEmail(normalizedEmail);

        if (existingUserOpt.isPresent()) {
            // === LUỒNG B1: Email đã tồn tại trong UniPath nhưng chưa liên kết Google ===
            User existingUser = existingUserOpt.get();
            log.info("Existing UniPath account found for email {}. Account linking required.", maskEmail(normalizedEmail));

            // Invalidate any previous unused link sessions for this googleSub to prevent session spam
            accountLinkSessionRepository.invalidateUnusedSessionsByGoogleSub(googleSub);

            // Generate cryptographically secure raw link token
            String rawLinkToken = secureTokenService.generateSecureToken();
            String linkTokenHash = secureTokenService.hashToken(rawLinkToken);
            LocalDateTime expiresAt = LocalDateTime.now().plus(LINK_SESSION_TTL);

            GoogleAccountLinkSession session = new GoogleAccountLinkSession(
                    linkTokenHash,
                    existingUser.getId(),
                    googleSub,
                    normalizedEmail,
                    expiresAt
            );
            accountLinkSessionRepository.save(session);

            return GoogleAuthResult.accountLinkRequired(rawLinkToken, normalizedEmail, expiresAt);
        }

        // === LUỒNG B2: Email hoàn toàn mới, chưa có tài khoản UniPath ===
        log.info("New Google user {}. Creating onboarding session.", maskEmail(normalizedEmail));

        // Invalidate any previous unused onboarding sessions for this googleSub
        onboardingSessionRepository.invalidateUnusedSessionsByGoogleSub(googleSub);

        // Generate cryptographically secure raw onboarding token
        String rawOnboardingToken = secureTokenService.generateSecureToken();
        String onboardingTokenHash = secureTokenService.hashToken(rawOnboardingToken);
        LocalDateTime expiresAt = LocalDateTime.now().plus(ONBOARDING_SESSION_TTL);

        GoogleOnboardingSession session = new GoogleOnboardingSession(
                onboardingTokenHash,
                googleSub,
                normalizedEmail,
                payload.getName(),
                payload.getPicture(),
                expiresAt
        );
        onboardingSessionRepository.save(session);

        return GoogleAuthResult.onboardingRequired(
                rawOnboardingToken,
                normalizedEmail,
                payload.getName(),
                payload.getPicture(),
                expiresAt
        );
    }

    private String maskEmail(String email) {
        if (email == null || email.isBlank()) return "***";
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) return "***" + (atIndex >= 0 ? email.substring(atIndex) : "");
        return email.charAt(0) + "***" + email.substring(atIndex);
    }

    private String maskSub(String sub) {
        if (sub == null || sub.length() <= 4) return "****";
        return sub.substring(0, 4) + "****";
    }
}


package com.example.unipathapi.auth.service;

import com.example.unipathapi.auth.dto.request.GoogleAccountLinkRequest;
import com.example.unipathapi.auth.dto.response.AuthResponse;
import com.example.unipathapi.auth.entity.GoogleAccountLinkSession;
import com.example.unipathapi.auth.entity.UserAuthProvider;
import com.example.unipathapi.auth.repository.GoogleAccountLinkSessionRepository;
import com.example.unipathapi.auth.repository.UserAuthProviderRepository;
import com.example.unipathapi.common.security.JwtUtil;
import com.example.unipathapi.user.entity.User;
import com.example.unipathapi.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;

/**
 * Service managing Google Account Linking for existing UniPath accounts.
 *
 * Rules:
 * - Requires cryptographic proof of linkToken session created during ACCOUNT_LINK_REQUIRED flow.
 * - Enforces account ownership verification via UniPath password re-authentication.
 * - Session consumption is strictly atomic (single-use token protection).
 * - Enforces unique provider constraints (1 Google Sub <-> 1 UniPath User).
 * - Rollbacks entire transaction if any verification step fails.
 * - Never logs raw link tokens, passwords, or sensitive credentials.
 */
@Service
public class GoogleAccountLinkService {

    private static final Logger log = LoggerFactory.getLogger(GoogleAccountLinkService.class);
    public static final String PROVIDER_GOOGLE = "GOOGLE";

    private final GoogleAccountLinkSessionRepository accountLinkSessionRepository;
    private final UserAuthProviderRepository userAuthProviderRepository;
    private final UserRepository userRepository;
    private final SecureTokenService secureTokenService;
    private final JwtUtil jwtUtil;

    public GoogleAccountLinkService(
            GoogleAccountLinkSessionRepository accountLinkSessionRepository,
            UserAuthProviderRepository userAuthProviderRepository,
            UserRepository userRepository,
            SecureTokenService secureTokenService,
            JwtUtil jwtUtil) {
        this.accountLinkSessionRepository = accountLinkSessionRepository;
        this.userAuthProviderRepository = userAuthProviderRepository;
        this.userRepository = userRepository;
        this.secureTokenService = secureTokenService;
        this.jwtUtil = jwtUtil;
    }

    /**
     * Executes atomic account linking of an existing UniPath user with a verified Google identity.
     *
     * @param request link request containing rawLinkToken and account password
     * @return AuthResponse containing new UniPath JWT
     */
    @Transactional
    public AuthResponse linkAccount(GoogleAccountLinkRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Dữ liệu yêu cầu liên kết không được để trống");
        }
        if (request.getLinkToken() == null || request.getLinkToken().trim().isEmpty()) {
            throw new IllegalArgumentException("Link token không được để trống");
        }
        if (request.getPassword() == null || request.getPassword().isEmpty()) {
            throw new IllegalArgumentException("Mật khẩu không được để trống");
        }

        // 1. Hash the raw link token
        String tokenHash = secureTokenService.hashToken(request.getLinkToken().trim());
        LocalDateTime now = LocalDateTime.now();

        // 2. Validate and atomically consume the link session
        GoogleAccountLinkSession session = validateAndConsumeSession(tokenHash, now);
        Integer userId = session.getUserId();
        String googleSub = session.getGoogleSub();

        // 3. Retrieve target UniPath user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Tài khoản người dùng không tồn tại"));

        // 4. Verify account active status
        if (Boolean.FALSE.equals(user.getIsActive())) {
            log.warn("Blocked account linking attempt for deactivated User ID: {}", user.getId());
            throw new IllegalArgumentException("Tài khoản của bạn đã bị khoá. Vui lòng liên hệ quản trị viên.");
        }

        // 5. Verify account ownership via password re-authentication (constant-time comparison)
        if (!verifyPassword(request.getPassword(), user.getPassword())) {
            log.warn("Account link failed: Incorrect password provided for User ID: {}", user.getId());
            throw new IllegalArgumentException("Mật khẩu tài khoản UniPath không chính xác");
        }

        // 6. Enforce provider constraints (application-level check)
        if (userAuthProviderRepository.existsByProviderAndProviderUserId(PROVIDER_GOOGLE, googleSub)) {
            log.warn("Account link failed: Google identity {} is already linked to another account", maskSub(googleSub));
            throw new IllegalStateException("Tài khoản Google này đã được liên kết với một tài khoản khác");
        }

        if (userAuthProviderRepository.existsByUserIdAndProvider(user.getId(), PROVIDER_GOOGLE)) {
            log.warn("Account link failed: User ID {} is already linked with a Google account", user.getId());
            throw new IllegalStateException("Tài khoản UniPath này đã được liên kết với tài khoản Google");
        }

        // 7. Create and persist UserAuthProvider with immediate flush to enforce DB constraints
        UserAuthProvider authProvider = new UserAuthProvider(user, PROVIDER_GOOGLE, googleSub, session.getEmail());
        try {
            userAuthProviderRepository.saveAndFlush(authProvider);
        } catch (DataIntegrityViolationException e) {
            log.warn("Database unique constraint violation during account linking for User ID: {}", user.getId(), e);
            throw new IllegalStateException("Tài khoản Google này hoặc tài khoản UniPath đã được liên kết", e);
        }

        // 8. Invalidate any remaining unused link sessions for this Google identity
        accountLinkSessionRepository.invalidateUnusedSessionsByGoogleSub(googleSub);

        log.info("Successfully linked Google identity to User ID: {}", user.getId());

        // 9. Issue standard UniPath JWT token
        String jwt = jwtUtil.generateToken(user.getId(), user.getRole());
        return new AuthResponse(
                jwt,
                String.valueOf(user.getId()),
                user.getRole(),
                "Liên kết tài khoản Google thành công!"
        );
    }

    /**
     * Atomically validates and consumes the link session using DB conditional update.
     * Prevents replay attacks and concurrent link attempts.
     */
    private GoogleAccountLinkSession validateAndConsumeSession(String tokenHash, LocalDateTime now) {
        GoogleAccountLinkSession session = accountLinkSessionRepository.findByLinkTokenHash(tokenHash)
                .orElseThrow(() -> new IllegalArgumentException("Phiên liên kết tài khoản không tồn tại hoặc không hợp lệ"));

        if (Boolean.TRUE.equals(session.getIsUsed())) {
            throw new IllegalStateException("Phiên liên kết tài khoản đã được sử dụng");
        }
        if (session.getExpiresAt().isBefore(now)) {
            throw new IllegalStateException("Phiên liên kết tài khoản đã hết hạn");
        }

        int updated = accountLinkSessionRepository.consumeSession(tokenHash, now);
        if (updated == 0) {
            throw new IllegalStateException("Phiên liên kết tài khoản đã được sử dụng hoặc đã hết hạn");
        }

        session.setIsUsed(true);
        return session;
    }

    /**
     * Verifies user password using constant-time byte comparison to protect against timing attacks.
     * Rejects null passwords safely (e.g. Google OAuth-only accounts without local password).
     */
    private boolean verifyPassword(String rawPassword, String userPassword) {
        if (userPassword == null || rawPassword == null) {
            return false;
        }
        byte[] userBytes = userPassword.getBytes(StandardCharsets.UTF_8);
        byte[] rawBytes = rawPassword.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(userBytes, rawBytes);
    }

    private String maskSub(String sub) {
        if (sub == null || sub.length() <= 4) return "****";
        return sub.substring(0, 4) + "****";
    }
}


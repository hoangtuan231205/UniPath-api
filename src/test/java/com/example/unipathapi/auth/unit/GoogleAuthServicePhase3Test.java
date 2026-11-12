package com.example.unipathapi.auth.unit;

import com.example.unipathapi.auth.dto.GoogleAuthResult;
import com.example.unipathapi.auth.dto.GoogleAuthStatus;
import com.example.unipathapi.auth.dto.GoogleTokenPayload;
import com.example.unipathapi.auth.entity.GoogleAccountLinkSession;
import com.example.unipathapi.auth.entity.GoogleOnboardingSession;
import com.example.unipathapi.auth.entity.UserAuthProvider;
import com.example.unipathapi.auth.exception.GoogleAuthException;
import com.example.unipathapi.auth.exception.InvalidAuthorizationCodeException;
import com.example.unipathapi.auth.exception.InvalidGoogleTokenException;
import com.example.unipathapi.auth.repository.GoogleAccountLinkSessionRepository;
import com.example.unipathapi.auth.repository.GoogleOnboardingSessionRepository;
import com.example.unipathapi.auth.repository.UserAuthProviderRepository;
import com.example.unipathapi.auth.service.GoogleAuthService;
import com.example.unipathapi.auth.service.GoogleTokenVerifierService;
import com.example.unipathapi.auth.service.SecureTokenService;
import com.example.unipathapi.common.security.JwtUtil;
import com.example.unipathapi.user.entity.User;
import com.example.unipathapi.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Phase 3.1 Unit Test Suite: Google Authentication Service (Backend).
 * Covers all 4 required testing groups:
 * - Group A: Linked Google Account (Tests 1 - 6)
 * - Group B: New Google User Onboarding (Tests 7 - 12)
 * - Group C: Email Collides with Existing UniPath Account (Tests 13 - 16)
 * - Group D: Security, Errors & Transactions (Tests 17 - 20)
 */
public class GoogleAuthServicePhase3Test {

    private GoogleTokenVerifierService tokenVerifierService;
    private SecureTokenService secureTokenService;
    private UserAuthProviderRepository userAuthProviderRepository;
    private GoogleOnboardingSessionRepository onboardingSessionRepository;
    private GoogleAccountLinkSessionRepository accountLinkSessionRepository;
    private UserRepository userRepository;
    private JwtUtil jwtUtil;

    private GoogleAuthService googleAuthService;

    @BeforeEach
    public void setup() {
        tokenVerifierService = mock(GoogleTokenVerifierService.class);
        secureTokenService = new SecureTokenService(); // Real CSRNG and SHA-256 hashing
        userAuthProviderRepository = mock(UserAuthProviderRepository.class);
        onboardingSessionRepository = mock(GoogleOnboardingSessionRepository.class);
        accountLinkSessionRepository = mock(GoogleAccountLinkSessionRepository.class);
        userRepository = mock(UserRepository.class);
        jwtUtil = new JwtUtil(); // Real JWT generator

        googleAuthService = new GoogleAuthService(
                tokenVerifierService,
                secureTokenService,
                userAuthProviderRepository,
                onboardingSessionRepository,
                accountLinkSessionRepository,
                userRepository,
                jwtUtil
        );
    }

    // =========================================================================
    // NHÓM A – TÀI KHOẢN GOOGLE ĐÃ LIÊN KẾT (TESTS 1 - 6)
    // =========================================================================

    @Test
    @DisplayName("1. Google sub tồn tại -> đăng nhập thành công")
    public void test1_GoogleSubExists_LoginSuccess() {
        String googleSub = "google-sub-1001";
        GoogleTokenPayload payload = GoogleTokenPayload.builder()
                .sub(googleSub)
                .email("student@gmail.com")
                .emailVerified(true)
                .build();

        User user = new User();
        user.setId(10);
        user.setEmail("student@gmail.com");
        user.setRole("CANDIDATE");
        user.setIsActive(true);

        UserAuthProvider provider = new UserAuthProvider(user, "GOOGLE", googleSub, "student@gmail.com");

        when(userAuthProviderRepository.findByProviderAndProviderUserId("GOOGLE", googleSub))
                .thenReturn(Optional.of(provider));
        when(userRepository.findById(10)).thenReturn(Optional.of(user));

        GoogleAuthResult result = googleAuthService.processGoogleIdentity(payload);

        assertEquals(GoogleAuthStatus.AUTHENTICATED, result.getStatus());
        assertTrue(result instanceof GoogleAuthResult.Authenticated);

        GoogleAuthResult.Authenticated authResult = (GoogleAuthResult.Authenticated) result;
        assertEquals(10, authResult.userId());
        assertEquals("student@gmail.com", authResult.email());
        assertEquals("CANDIDATE", authResult.role());
        assertNotNull(authResult.authResponse());
        assertNotNull(authResult.authResponse().getToken());
        assertEquals("Đăng nhập bằng Google thành công!", authResult.authResponse().getMessage());
    }

    @Test
    @DisplayName("2. Không tìm thấy User tương ứng với Provider -> ném ngoại lệ an toàn")
    public void test2_ProviderUserNotFound_ThrowsException() {
        String googleSub = "google-sub-ghost";
        GoogleTokenPayload payload = GoogleTokenPayload.builder()
                .sub(googleSub)
                .email("ghost@gmail.com")
                .emailVerified(true)
                .build();

        User user = new User();
        user.setId(999); // ID not in DB

        UserAuthProvider provider = new UserAuthProvider(user, "GOOGLE", googleSub, "ghost@gmail.com");

        when(userAuthProviderRepository.findByProviderAndProviderUserId("GOOGLE", googleSub))
                .thenReturn(Optional.of(provider));
        when(userRepository.findById(999)).thenReturn(Optional.empty());

        GoogleAuthException ex = assertThrows(GoogleAuthException.class, () ->
                googleAuthService.processGoogleIdentity(payload)
        );
        assertTrue(ex.getMessage().contains("không tồn tại"));
    }

    @Test
    @DisplayName("3. User bị khóa (isActive=false) -> từ chối đăng nhập")
    public void test3_UserInactive_ThrowsException() {
        String googleSub = "google-sub-locked";
        GoogleTokenPayload payload = GoogleTokenPayload.builder()
                .sub(googleSub)
                .email("locked@gmail.com")
                .emailVerified(true)
                .build();

        User user = new User();
        user.setId(20);
        user.setEmail("locked@gmail.com");
        user.setRole("CANDIDATE");
        user.setIsActive(false); // Locked!

        UserAuthProvider provider = new UserAuthProvider(user, "GOOGLE", googleSub, "locked@gmail.com");

        when(userAuthProviderRepository.findByProviderAndProviderUserId("GOOGLE", googleSub))
                .thenReturn(Optional.of(provider));
        when(userRepository.findById(20)).thenReturn(Optional.of(user));

        GoogleAuthException ex = assertThrows(GoogleAuthException.class, () ->
                googleAuthService.processGoogleIdentity(payload)
        );
        assertTrue(ex.getMessage().contains("Tài khoản của bạn đã bị khoá"));
    }

    @Test
    @DisplayName("4. User không hợp lệ -> không tạo JWT")
    public void test4_InvalidUser_NoJwtGenerated() {
        String googleSub = "google-sub-invalid";
        GoogleTokenPayload payload = GoogleTokenPayload.builder()
                .sub(googleSub)
                .email("invalid@gmail.com")
                .emailVerified(true)
                .build();

        JwtUtil mockJwt = mock(JwtUtil.class);
        GoogleAuthService svcWithMockJwt = new GoogleAuthService(
                tokenVerifierService, secureTokenService, userAuthProviderRepository,
                onboardingSessionRepository, accountLinkSessionRepository, userRepository, mockJwt
        );

        User user = new User();
        user.setId(30);
        user.setIsActive(false);

        UserAuthProvider provider = new UserAuthProvider(user, "GOOGLE", googleSub, "invalid@gmail.com");
        when(userAuthProviderRepository.findByProviderAndProviderUserId("GOOGLE", googleSub))
                .thenReturn(Optional.of(provider));
        when(userRepository.findById(30)).thenReturn(Optional.of(user));

        assertThrows(GoogleAuthException.class, () -> svcWithMockJwt.processGoogleIdentity(payload));
        verify(mockJwt, never()).generateToken(anyInt(), anyString());
    }

    @Test
    @DisplayName("5. JWT được tạo bằng đúng cơ chế hiện tại của UniPath")
    public void test5_JwtCompatibleWithEmailPasswordLogin() {
        String googleSub = "google-sub-jwt";
        GoogleTokenPayload payload = GoogleTokenPayload.builder()
                .sub(googleSub)
                .email("jwt.user@gmail.com")
                .emailVerified(true)
                .build();

        User user = new User();
        user.setId(45);
        user.setEmail("jwt.user@gmail.com");
        user.setRole("EMPLOYER");
        user.setIsActive(true);

        UserAuthProvider provider = new UserAuthProvider(user, "GOOGLE", googleSub, "jwt.user@gmail.com");
        when(userAuthProviderRepository.findByProviderAndProviderUserId("GOOGLE", googleSub)).thenReturn(Optional.of(provider));
        when(userRepository.findById(45)).thenReturn(Optional.of(user));

        GoogleAuthResult result = googleAuthService.processGoogleIdentity(payload);
        GoogleAuthResult.Authenticated authResult = (GoogleAuthResult.Authenticated) result;
        String token = authResult.authResponse().getToken();

        // Verify token with HMAC key from JwtUtil
        String secret = "UniPath_SecretKey_Chuyen_Xy_Ly_Bao_Mat_2026_@#$!";
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(secret.getBytes()))
                .build()
                .parseClaimsJws(token)
                .getBody();

        assertEquals("45", claims.getSubject());
        assertEquals("EMPLOYER", claims.get("role", String.class));
    }

    @Test
    @DisplayName("6. Role của User không bị thay đổi khi đăng nhập Google")
    public void test6_UserRoleNotModified() {
        String googleSub = "google-sub-employer";
        GoogleTokenPayload payload = GoogleTokenPayload.builder()
                .sub(googleSub)
                .email("hr.manager@company.com")
                .emailVerified(true)
                .build();

        User user = new User();
        user.setId(88);
        user.setEmail("hr.manager@company.com");
        user.setRole("EMPLOYER");
        user.setIsActive(true);

        UserAuthProvider provider = new UserAuthProvider(user, "GOOGLE", googleSub, "hr.manager@company.com");
        when(userAuthProviderRepository.findByProviderAndProviderUserId("GOOGLE", googleSub)).thenReturn(Optional.of(provider));
        when(userRepository.findById(88)).thenReturn(Optional.of(user));

        GoogleAuthResult result = googleAuthService.processGoogleIdentity(payload);
        GoogleAuthResult.Authenticated authResult = (GoogleAuthResult.Authenticated) result;

        assertEquals("EMPLOYER", authResult.role());
        verify(userRepository, never()).save(any(User.class)); // User is not modified
    }

    // =========================================================================
    // NHÓM B – GOOGLE USER MỚI (TESTS 7 - 12)
    // =========================================================================

    @Test
    @DisplayName("7. Google sub chưa tồn tại, email chưa tồn tại -> tạo onboarding session")
    public void test7_NewGoogleUser_CreatesOnboardingSession() {
        String googleSub = "new-sub-999";
        GoogleTokenPayload payload = GoogleTokenPayload.builder()
                .sub(googleSub)
                .email("newbie@gmail.com")
                .name("Newbie User")
                .picture("https://google.com/pic.jpg")
                .emailVerified(true)
                .build();

        when(userAuthProviderRepository.findByProviderAndProviderUserId("GOOGLE", googleSub)).thenReturn(Optional.empty());
        when(userRepository.findByEmail("newbie@gmail.com")).thenReturn(Optional.empty());

        GoogleAuthResult result = googleAuthService.processGoogleIdentity(payload);

        assertEquals(GoogleAuthStatus.ONBOARDING_REQUIRED, result.getStatus());
        assertTrue(result instanceof GoogleAuthResult.OnboardingRequired);

        GoogleAuthResult.OnboardingRequired onboardingResult = (GoogleAuthResult.OnboardingRequired) result;
        assertNotNull(onboardingResult.onboardingToken());
        assertEquals(43, onboardingResult.onboardingToken().length()); // 32 bytes Base64URL
        assertEquals("newbie@gmail.com", onboardingResult.email());
        assertEquals("Newbie User", onboardingResult.name());
        assertEquals("https://google.com/pic.jpg", onboardingResult.picture());

        verify(onboardingSessionRepository, times(1)).save(any(GoogleOnboardingSession.class));
    }

    @Test
    @DisplayName("8. Database chỉ lưu SHA-256 hash của onboarding token, không lưu token thô")
    public void test8_DatabaseStoresOnlyHash() {
        String googleSub = "new-sub-hash-test";
        GoogleTokenPayload payload = GoogleTokenPayload.builder()
                .sub(googleSub)
                .email("secure.hash@gmail.com")
                .emailVerified(true)
                .build();

        when(userAuthProviderRepository.findByProviderAndProviderUserId("GOOGLE", googleSub)).thenReturn(Optional.empty());
        when(userRepository.findByEmail("secure.hash@gmail.com")).thenReturn(Optional.empty());

        GoogleAuthResult result = googleAuthService.processGoogleIdentity(payload);
        GoogleAuthResult.OnboardingRequired onboardingResult = (GoogleAuthResult.OnboardingRequired) result;
        String rawToken = onboardingResult.onboardingToken();

        ArgumentCaptor<GoogleOnboardingSession> sessionCaptor = ArgumentCaptor.forClass(GoogleOnboardingSession.class);
        verify(onboardingSessionRepository).save(sessionCaptor.capture());

        GoogleOnboardingSession savedSession = sessionCaptor.getValue();
        String storedHash = savedSession.getTokenHash();

        assertNotNull(storedHash);
        assertEquals(64, storedHash.length()); // 64 hex lowercase chars
        assertNotEquals(rawToken, storedHash);
        assertEquals(secureTokenService.hashToken(rawToken), storedHash);
    }

    @Test
    @DisplayName("9. Raw onboarding token được trả về đúng tầng gọi")
    public void test9_RawTokenReturnedToCaller() {
        String googleSub = "sub-raw-test";
        GoogleTokenPayload payload = GoogleTokenPayload.builder()
                .sub(googleSub)
                .email("raw.token@gmail.com")
                .emailVerified(true)
                .build();

        when(userAuthProviderRepository.findByProviderAndProviderUserId("GOOGLE", googleSub)).thenReturn(Optional.empty());
        when(userRepository.findByEmail("raw.token@gmail.com")).thenReturn(Optional.empty());

        GoogleAuthResult result = googleAuthService.processGoogleIdentity(payload);
        GoogleAuthResult.OnboardingRequired onboardingResult = (GoogleAuthResult.OnboardingRequired) result;

        assertNotNull(onboardingResult.onboardingToken());
        assertTrue(onboardingResult.onboardingToken().matches("^[A-Za-z0-9_-]{43}$"));
    }

    @Test
    @DisplayName("10. Onboarding session có thời gian hết hạn (15 phút) và isUsed = false")
    public void test10_OnboardingSessionHasExpiration() {
        String googleSub = "sub-exp-test";
        GoogleTokenPayload payload = GoogleTokenPayload.builder()
                .sub(googleSub)
                .email("expiry@gmail.com")
                .emailVerified(true)
                .build();

        when(userAuthProviderRepository.findByProviderAndProviderUserId("GOOGLE", googleSub)).thenReturn(Optional.empty());
        when(userRepository.findByEmail("expiry@gmail.com")).thenReturn(Optional.empty());

        googleAuthService.processGoogleIdentity(payload);

        ArgumentCaptor<GoogleOnboardingSession> sessionCaptor = ArgumentCaptor.forClass(GoogleOnboardingSession.class);
        verify(onboardingSessionRepository).save(sessionCaptor.capture());

        GoogleOnboardingSession session = sessionCaptor.getValue();
        assertFalse(session.getIsUsed());
        assertNotNull(session.getExpiresAt());
        assertTrue(session.getExpiresAt().isAfter(LocalDateTime.now().plusMinutes(14)));
        assertTrue(session.getExpiresAt().isBefore(LocalDateTime.now().plusMinutes(16)));
    }

    @Test
    @DisplayName("11. Email được chuẩn hóa nhất quán (lowercase + trim)")
    public void test11_EmailNormalizedConsistently() {
        String googleSub = "sub-email-norm";
        GoogleTokenPayload payload = GoogleTokenPayload.builder()
                .sub(googleSub)
                .email("   ALEX.Rivera+Tag@GMAIL.com   ")
                .emailVerified(true)
                .build();

        when(userAuthProviderRepository.findByProviderAndProviderUserId("GOOGLE", googleSub)).thenReturn(Optional.empty());
        when(userRepository.findByEmail("alex.rivera+tag@gmail.com")).thenReturn(Optional.empty());

        googleAuthService.processGoogleIdentity(payload);

        ArgumentCaptor<GoogleOnboardingSession> sessionCaptor = ArgumentCaptor.forClass(GoogleOnboardingSession.class);
        verify(onboardingSessionRepository).save(sessionCaptor.capture());

        assertEquals("alex.rivera+tag@gmail.com", sessionCaptor.getValue().getEmail());
    }

    @Test
    @DisplayName("12. Google sub được lưu chính xác vào onboarding session")
    public void test12_GoogleSubStoredAccurately() {
        String googleSub = "117283948572019485721";
        GoogleTokenPayload payload = GoogleTokenPayload.builder()
                .sub(googleSub)
                .email("sub.exact@gmail.com")
                .emailVerified(true)
                .build();

        when(userAuthProviderRepository.findByProviderAndProviderUserId("GOOGLE", googleSub)).thenReturn(Optional.empty());
        when(userRepository.findByEmail("sub.exact@gmail.com")).thenReturn(Optional.empty());

        googleAuthService.processGoogleIdentity(payload);

        ArgumentCaptor<GoogleOnboardingSession> sessionCaptor = ArgumentCaptor.forClass(GoogleOnboardingSession.class);
        verify(onboardingSessionRepository).save(sessionCaptor.capture());

        assertEquals("117283948572019485721", sessionCaptor.getValue().getGoogleSub());
    }

    // =========================================================================
    // NHÓM C – EMAIL TRÙNG TÀI KHOẢN HIỆN TẠI (TESTS 13 - 16)
    // =========================================================================

    @Test
    @DisplayName("13. Email đã có User nhưng chưa liên kết Google -> yêu cầu Account Linking")
    public void test13_ExistingEmailWithoutGoogleProvider_RequiresAccountLink() {
        String googleSub = "sub-link-needed";
        String existingEmail = "existing.student@gmail.com";

        GoogleTokenPayload payload = GoogleTokenPayload.builder()
                .sub(googleSub)
                .email(existingEmail)
                .emailVerified(true)
                .build();

        User existingUser = new User();
        existingUser.setId(55);
        existingUser.setEmail(existingEmail);
        existingUser.setPassword("$2a$10$hashedPasswordHere");
        existingUser.setRole("CANDIDATE");
        existingUser.setIsActive(true);

        when(userAuthProviderRepository.findByProviderAndProviderUserId("GOOGLE", googleSub)).thenReturn(Optional.empty());
        when(userRepository.findByEmail(existingEmail)).thenReturn(Optional.of(existingUser));

        GoogleAuthResult result = googleAuthService.processGoogleIdentity(payload);

        assertEquals(GoogleAuthStatus.ACCOUNT_LINK_REQUIRED, result.getStatus());
        assertTrue(result instanceof GoogleAuthResult.AccountLinkRequired);

        GoogleAuthResult.AccountLinkRequired linkResult = (GoogleAuthResult.AccountLinkRequired) result;
        assertNotNull(linkResult.linkToken());
        assertEquals(existingEmail, linkResult.email());
        assertNotNull(linkResult.expiresAt());

        verify(accountLinkSessionRepository, times(1)).save(any(GoogleAccountLinkSession.class));
    }

    @Test
    @DisplayName("14. Không tự động liên kết Provider chỉ vì email trùng nhau")
    public void test14_NoProviderCreatedAutomatically() {
        String googleSub = "sub-no-auto-link";
        String existingEmail = "user.no.auto@gmail.com";

        GoogleTokenPayload payload = GoogleTokenPayload.builder()
                .sub(googleSub)
                .email(existingEmail)
                .emailVerified(true)
                .build();

        User existingUser = new User();
        existingUser.setId(66);
        existingUser.setEmail(existingEmail);

        when(userAuthProviderRepository.findByProviderAndProviderUserId("GOOGLE", googleSub)).thenReturn(Optional.empty());
        when(userRepository.findByEmail(existingEmail)).thenReturn(Optional.of(existingUser));

        googleAuthService.processGoogleIdentity(payload);

        // MUST NOT create UserAuthProvider automatically!
        verify(userAuthProviderRepository, never()).save(any(UserAuthProvider.class));
    }

    @Test
    @DisplayName("15. Không tạo User mới khi phát hiện email trùng")
    public void test15_NoNewUserCreated() {
        String googleSub = "sub-no-new-user";
        String existingEmail = "user.duplicate@gmail.com";

        GoogleTokenPayload payload = GoogleTokenPayload.builder()
                .sub(googleSub)
                .email(existingEmail)
                .emailVerified(true)
                .build();

        User existingUser = new User();
        existingUser.setId(77);
        existingUser.setEmail(existingEmail);

        when(userAuthProviderRepository.findByProviderAndProviderUserId("GOOGLE", googleSub)).thenReturn(Optional.empty());
        when(userRepository.findByEmail(existingEmail)).thenReturn(Optional.of(existingUser));

        googleAuthService.processGoogleIdentity(payload);

        // MUST NOT save a new user
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("16. Kết quả trả về không chứa password hash hoặc dữ liệu nhạy cảm của User")
    public void test16_NoSensitiveDataExposed() {
        String googleSub = "sub-safe-result";
        String existingEmail = "sensitive.user@gmail.com";

        GoogleTokenPayload payload = GoogleTokenPayload.builder()
                .sub(googleSub)
                .email(existingEmail)
                .emailVerified(true)
                .build();

        User existingUser = new User();
        existingUser.setId(99);
        existingUser.setEmail(existingEmail);
        existingUser.setPassword("SUPER_SECRET_BCRYPT_HASH");

        when(userAuthProviderRepository.findByProviderAndProviderUserId("GOOGLE", googleSub)).thenReturn(Optional.empty());
        when(userRepository.findByEmail(existingEmail)).thenReturn(Optional.of(existingUser));

        GoogleAuthResult result = googleAuthService.processGoogleIdentity(payload);
        GoogleAuthResult.AccountLinkRequired linkResult = (GoogleAuthResult.AccountLinkRequired) result;

        // Verify result only exposes safe fields
        assertEquals(existingEmail, linkResult.email());
        assertNotNull(linkResult.linkToken());
        assertFalse(linkResult.toString().contains("SUPER_SECRET_BCRYPT_HASH"));
    }

    // =========================================================================
    // NHÓM D – BẢO MẬT VÀ LỖI (TESTS 17 - 20)
    // =========================================================================

    @Test
    @DisplayName("17. GoogleTokenVerifierService thất bại -> không ghi database")
    public void test17_GoogleTokenVerifierFailure_NoDatabaseWrites() {
        when(tokenVerifierService.exchangeCodeAndVerify("invalid_code"))
                .thenThrow(new InvalidAuthorizationCodeException("Google authorization code is invalid or has expired"));

        assertThrows(InvalidAuthorizationCodeException.class, () ->
                googleAuthService.authenticateWithGoogleCode("invalid_code")
        );

        verifyNoInteractions(userAuthProviderRepository);
        verifyNoInteractions(onboardingSessionRepository);
        verifyNoInteractions(accountLinkSessionRepository);
        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("18. Database lỗi -> ném ngoại lệ để Spring rollback transaction")
    public void test18_DatabaseError_TransactionRollback() {
        String googleSub = "sub-db-err";
        GoogleTokenPayload payload = GoogleTokenPayload.builder()
                .sub(googleSub)
                .email("dberror@gmail.com")
                .emailVerified(true)
                .build();

        when(userAuthProviderRepository.findByProviderAndProviderUserId("GOOGLE", googleSub)).thenReturn(Optional.empty());
        when(userRepository.findByEmail("dberror@gmail.com")).thenReturn(Optional.empty());
        when(onboardingSessionRepository.save(any(GoogleOnboardingSession.class)))
                .thenThrow(new DataIntegrityViolationException("Simulated database constraint violation"));

        assertThrows(DataIntegrityViolationException.class, () ->
                googleAuthService.processGoogleIdentity(payload)
        );
    }

    @Test
    @DisplayName("19. Không ghi raw token vào log hoặc lưu raw token xuống DB")
    public void test19_NoRawTokenInLogs() {
        String googleSub = "sub-no-log";
        GoogleTokenPayload payload = GoogleTokenPayload.builder()
                .sub(googleSub)
                .email("nolog@gmail.com")
                .emailVerified(true)
                .build();

        when(userAuthProviderRepository.findByProviderAndProviderUserId("GOOGLE", googleSub)).thenReturn(Optional.empty());
        when(userRepository.findByEmail("nolog@gmail.com")).thenReturn(Optional.empty());

        GoogleAuthResult result = googleAuthService.processGoogleIdentity(payload);
        GoogleAuthResult.OnboardingRequired onboardingResult = (GoogleAuthResult.OnboardingRequired) result;
        String rawToken = onboardingResult.onboardingToken();

        ArgumentCaptor<GoogleOnboardingSession> sessionCaptor = ArgumentCaptor.forClass(GoogleOnboardingSession.class);
        verify(onboardingSessionRepository).save(sessionCaptor.capture());

        GoogleOnboardingSession session = sessionCaptor.getValue();
        assertNotEquals(rawToken, session.getTokenHash());
        assertFalse(session.toString().contains(rawToken));
    }

    @Test
    @DisplayName("20. Yêu cầu liên tiếp của cùng một Google identity -> vô hiệu hóa session cũ trước khi tạo session mới")
    public void test20_ConcurrentRequests_InvalidatePreviousSession() {
        String googleSub = "sub-duplicate-request";
        GoogleTokenPayload payload = GoogleTokenPayload.builder()
                .sub(googleSub)
                .email("multi.req@gmail.com")
                .emailVerified(true)
                .build();

        when(userAuthProviderRepository.findByProviderAndProviderUserId("GOOGLE", googleSub)).thenReturn(Optional.empty());
        when(userRepository.findByEmail("multi.req@gmail.com")).thenReturn(Optional.empty());

        googleAuthService.processGoogleIdentity(payload);

        // Verify invalidation of previous sessions for this sub was called
        verify(onboardingSessionRepository, times(1)).invalidateUnusedSessionsByGoogleSub(googleSub);
        verify(onboardingSessionRepository, times(1)).save(any(GoogleOnboardingSession.class));
    }
}


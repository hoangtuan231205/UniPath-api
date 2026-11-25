package com.example.unipathapi.auth.unit;

import com.example.unipathapi.auth.controller.GoogleAccountLinkController;
import com.example.unipathapi.auth.dto.request.GoogleCandidateOnboardingRequest;
import com.example.unipathapi.auth.dto.request.GoogleEmployerOnboardingRequest;
import com.example.unipathapi.auth.dto.request.GoogleOnboardingContextRequest;
import com.example.unipathapi.auth.dto.response.AuthResponse;
import com.example.unipathapi.auth.dto.response.GoogleOnboardingContextResponse;
import com.example.unipathapi.auth.entity.GoogleOnboardingSession;
import com.example.unipathapi.auth.exception.GoogleAuthException;
import com.example.unipathapi.auth.repository.GoogleOnboardingSessionRepository;
import com.example.unipathapi.auth.repository.UserAuthProviderRepository;
import com.example.unipathapi.auth.service.GoogleAccountLinkService;
import com.example.unipathapi.auth.service.GoogleAuthService;
import com.example.unipathapi.auth.service.GoogleOAuthSecurityValidator;
import com.example.unipathapi.auth.service.GoogleOnboardingService;
import com.example.unipathapi.auth.service.SecureTokenService;
import com.example.unipathapi.candidate.repository.CandidateProfileRepository;
import com.example.unipathapi.common.exception.GlobalExceptionHandler;
import com.example.unipathapi.common.security.JwtUtil;
import com.example.unipathapi.company.repository.CompanyJoinRequestRepository;
import com.example.unipathapi.company.repository.CompanyRepository;
import com.example.unipathapi.company.service.CompanyManagementService;
import com.example.unipathapi.employer.repository.EmployerProfileRepository;
import com.example.unipathapi.user.entity.User;
import com.example.unipathapi.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit & MockMvc Tests for Google Signup Onboarding Context & Completion Endpoints.
 * Covers:
 * - Service-level getOnboardingContext validation (valid, expired, reused, duplicate email, duplicate sub, non-consuming)
 * - Controller endpoints:
 *   - POST /api/auth/google/onboarding/context
 *   - POST /api/auth/google/onboarding/candidate
 *   - POST /api/auth/google/onboarding/employer
 * - Security checks: no googleSub/tokenHash exposure, CSRF X-Requested-With & Origin validation, client identity spoofing ignored.
 */
public class GoogleOnboardingControllerAndContextTest {

    private GoogleOnboardingSessionRepository onboardingSessionRepository;
    private UserRepository userRepository;
    private UserAuthProviderRepository userAuthProviderRepository;
    private SecureTokenService secureTokenService;
    private GoogleOnboardingService realOnboardingService;

    private GoogleOnboardingService mockOnboardingService;
    private GoogleOAuthSecurityValidator mockSecurityValidator;
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private static final String RAW_TOKEN = "raw-onboarding-token-xyz-999";
    private static final String TOKEN_HASH = "sha256_hashed_onboarding_token_999";
    private static final String GOOGLE_SUB = "google-sub-verified-8888";
    private static final String GOOGLE_EMAIL = "verified.student@gmail.com";
    private static final String GOOGLE_NAME = "Nguyễn Minh Anh";
    private static final String GOOGLE_PICTURE = "https://lh3.googleusercontent.com/a/verified-avatar.png";

    @BeforeEach
    void setUp() {
        onboardingSessionRepository = mock(GoogleOnboardingSessionRepository.class);
        userRepository = mock(UserRepository.class);
        userAuthProviderRepository = mock(UserAuthProviderRepository.class);
        secureTokenService = mock(SecureTokenService.class);

        realOnboardingService = new GoogleOnboardingService(
                onboardingSessionRepository,
                userRepository,
                mock(CandidateProfileRepository.class),
                mock(EmployerProfileRepository.class),
                mock(CompanyRepository.class),
                mock(CompanyJoinRequestRepository.class),
                mock(CompanyManagementService.class),
                userAuthProviderRepository,
                secureTokenService,
                mock(JwtUtil.class)
        );
        when(secureTokenService.hashToken(RAW_TOKEN)).thenReturn(TOKEN_HASH);

        mockOnboardingService = mock(GoogleOnboardingService.class);
        mockSecurityValidator = mock(GoogleOAuthSecurityValidator.class);

        GoogleAccountLinkController controller = new GoogleAccountLinkController(
                mock(GoogleAccountLinkService.class),
                mock(GoogleAuthService.class),
                mockSecurityValidator,
                mockOnboardingService
        );

        objectMapper = new ObjectMapper();

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private GoogleOnboardingSession buildActiveSession() {
        GoogleOnboardingSession s = new GoogleOnboardingSession(
                TOKEN_HASH,
                GOOGLE_SUB,
                GOOGLE_EMAIL,
                GOOGLE_NAME,
                GOOGLE_PICTURE,
                LocalDateTime.now().plusMinutes(15)
        );
        s.setId(77);
        s.setIsUsed(false);
        return s;
    }

    // =========================================================================
    // SERVICE CONTEXT RETRIEVAL TESTS
    // =========================================================================

    @Test
    @DisplayName("1. getOnboardingContext returns verified email, name, picture without consuming session")
    void testGetOnboardingContext_ValidSession_ReturnsProfileWithoutConsuming() {
        GoogleOnboardingSession session = buildActiveSession();
        when(onboardingSessionRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(session));
        when(userRepository.findByEmail(GOOGLE_EMAIL)).thenReturn(Optional.empty());
        when(userAuthProviderRepository.existsByProviderAndProviderUserId("GOOGLE", GOOGLE_SUB)).thenReturn(false);

        GoogleOnboardingContextResponse ctx = realOnboardingService.getOnboardingContext(RAW_TOKEN);

        assertEquals(GOOGLE_EMAIL, ctx.getEmail());
        assertEquals(GOOGLE_NAME, ctx.getName());
        assertEquals(GOOGLE_PICTURE, ctx.getPicture());
        assertNotNull(ctx.getExpiresAt());
        verify(onboardingSessionRepository, never()).consumeSession(any(), any());
    }

    @Test
    @DisplayName("2. getOnboardingContext rejects expired onboarding token")
    void testGetOnboardingContext_ExpiredToken_ThrowsIllegalState() {
        GoogleOnboardingSession expired = buildActiveSession();
        expired.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        when(onboardingSessionRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(expired));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> realOnboardingService.getOnboardingContext(RAW_TOKEN));
        assertTrue(ex.getMessage().contains("hết hạn"));
    }

    @Test
    @DisplayName("3. getOnboardingContext rejects already-used onboarding token")
    void testGetOnboardingContext_UsedToken_ThrowsIllegalState() {
        GoogleOnboardingSession used = buildActiveSession();
        used.setIsUsed(true);
        when(onboardingSessionRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(used));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> realOnboardingService.getOnboardingContext(RAW_TOKEN));
        assertTrue(ex.getMessage().contains("đã được sử dụng"));
    }

    @Test
    @DisplayName("4. getOnboardingContext rejects duplicate Google email or Google sub")
    void testGetOnboardingContext_DuplicateEmailOrSub_ThrowsIllegalState() {
        GoogleOnboardingSession session = buildActiveSession();
        when(onboardingSessionRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(session));

        User existing = new User();
        existing.setId(55);
        existing.setEmail(GOOGLE_EMAIL);
        when(userRepository.findByEmail(GOOGLE_EMAIL)).thenReturn(Optional.of(existing));

        assertThrows(IllegalStateException.class, () -> realOnboardingService.getOnboardingContext(RAW_TOKEN));

        when(userRepository.findByEmail(GOOGLE_EMAIL)).thenReturn(Optional.empty());
        when(userAuthProviderRepository.existsByProviderAndProviderUserId("GOOGLE", GOOGLE_SUB)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> realOnboardingService.getOnboardingContext(RAW_TOKEN));
    }

    @Test
    @DisplayName("5. getOnboardingContext sanitizes non-HTTP picture URLs (prevents javascript: XSS)")
    void testGetOnboardingContext_JavascriptPictureUrl_SanitizedToNull() {
        GoogleOnboardingSession session = buildActiveSession();
        session.setPicture("javascript:alert(1)");
        when(onboardingSessionRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(session));
        when(userRepository.findByEmail(GOOGLE_EMAIL)).thenReturn(Optional.empty());
        when(userAuthProviderRepository.existsByProviderAndProviderUserId("GOOGLE", GOOGLE_SUB)).thenReturn(false);

        GoogleOnboardingContextResponse ctx = realOnboardingService.getOnboardingContext(RAW_TOKEN);
        assertNull(ctx.getPicture());
    }

    // =========================================================================
    // CONTROLLER ENDPOINT & SECURITY TESTS
    // =========================================================================

    @Test
    @DisplayName("6. POST /api/auth/google/onboarding/context returns 200 OK and never leaks googleSub or tokenHash")
    void testContextEndpoint_Success_NoSensitiveFieldsExposed() throws Exception {
        GoogleOnboardingContextResponse mockCtx = GoogleOnboardingContextResponse.builder()
                .email(GOOGLE_EMAIL)
                .name(GOOGLE_NAME)
                .picture(GOOGLE_PICTURE)
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .build();

        when(mockOnboardingService.getOnboardingContext(RAW_TOKEN)).thenReturn(mockCtx);

        mockMvc.perform(post("/api/auth/google/onboarding/context")
                        .header("Origin", "http://localhost:5500")
                        .header("X-Requested-With", "XmlHttpRequest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new GoogleOnboardingContextRequest(RAW_TOKEN))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is(GOOGLE_EMAIL)))
                .andExpect(jsonPath("$.name", is(GOOGLE_NAME)))
                .andExpect(jsonPath("$.picture", is(GOOGLE_PICTURE)))
                .andExpect(jsonPath("$.googleSub").doesNotExist())
                .andExpect(jsonPath("$.sub").doesNotExist())
                .andExpect(jsonPath("$.tokenHash").doesNotExist())
                .andExpect(jsonPath("$.onboardingToken").doesNotExist());

        verify(mockSecurityValidator, times(1)).validatePopupHeaders("http://localhost:5500", "XmlHttpRequest");
    }

    @Test
    @DisplayName("7. POST /api/auth/google/onboarding/context rejects missing CSRF header when Origin is present")
    void testContextEndpoint_MissingCsrfHeader_Rejected() throws Exception {
        doThrow(new GoogleAuthException("Missing or invalid X-Requested-With header"))
                .when(mockSecurityValidator).validatePopupHeaders("http://localhost:5500", null);

        mockMvc.perform(post("/api/auth/google/onboarding/context")
                        .header("Origin", "http://localhost:5500")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"onboardingToken\":\"" + RAW_TOKEN + "\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(mockOnboardingService);
    }

    @Test
    @DisplayName("8. POST /api/auth/google/onboarding/candidate completes signup, ignores spoofed email/sub, returns JWT")
    void testCandidateEndpoint_Success_IgnoresSpoofedIdentity() throws Exception {
        AuthResponse mockAuth = new AuthResponse("jwt.candidate.777", "205", "CANDIDATE", "Đăng ký tài khoản Ứng viên bằng Google thành công!");
        when(mockOnboardingService.completeCandidateOnboarding(any(GoogleCandidateOnboardingRequest.class))).thenReturn(mockAuth);

        String payloadWithSpoofedFields = "{"
                + "\"onboardingToken\":\"" + RAW_TOKEN + "\","
                + "\"fullName\":\"Nguyễn Minh Anh\","
                + "\"phoneNumber\":\"0988112233\","
                + "\"universityName\":\"Đại học Bách Khoa\","
                + "\"major\":\"Khoa học máy tính\","
                + "\"experienceYears\":0,"
                + "\"skills\":\"Java, Spring Boot\","
                + "\"email\":\"attacker@evil.com\","
                + "\"googleSub\":\"spoofed-sub\","
                + "\"role\":\"SUPERADMIN\""
                + "}";

        mockMvc.perform(post("/api/auth/google/onboarding/candidate")
                        .header("Origin", "http://localhost:5500")
                        .header("X-Requested-With", "XmlHttpRequest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadWithSpoofedFields))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", is("jwt.candidate.777")))
                .andExpect(jsonPath("$.userID", is("205")))
                .andExpect(jsonPath("$.role", is("CANDIDATE")));

        verify(mockOnboardingService, times(1)).completeCandidateOnboarding(argThat(req ->
                RAW_TOKEN.equals(req.getOnboardingToken())
                        && "Nguyễn Minh Anh".equals(req.getFullName())
                        && Integer.valueOf(0).equals(req.getExperienceYears())
        ));
    }

    @Test
    @DisplayName("9. POST /api/auth/google/onboarding/employer completes signup and returns EMPLOYER JWT")
    void testEmployerEndpoint_Success_ReturnsEmployerJwt() throws Exception {
        AuthResponse mockAuth = new AuthResponse("jwt.employer.888", "309", "EMPLOYER", "Đăng ký tài khoản Nhà tuyển dụng bằng Google thành công!");
        when(mockOnboardingService.completeEmployerOnboarding(any(GoogleEmployerOnboardingRequest.class))).thenReturn(mockAuth);

        GoogleEmployerOnboardingRequest req = GoogleEmployerOnboardingRequest.builder()
                .onboardingToken(RAW_TOKEN)
                .fullName("Trần Thị HR")
                .phone("0912345678")
                .position("HR Manager")
                .companyId(12)
                .build();

        mockMvc.perform(post("/api/auth/google/onboarding/employer")
                        .header("Origin", "http://localhost:5500")
                        .header("X-Requested-With", "XmlHttpRequest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", is("jwt.employer.888")))
                .andExpect(jsonPath("$.userID", is("309")))
                .andExpect(jsonPath("$.role", is("EMPLOYER")));
    }

    @Test
    @DisplayName("10. POST /api/auth/google/onboarding/candidate returns 409 Conflict when token is expired or reused")
    void testCandidateEndpoint_ExpiredOrReusedToken_Returns409() throws Exception {
        when(mockOnboardingService.completeCandidateOnboarding(any()))
                .thenThrow(new IllegalStateException("Onboarding session đã được sử dụng"));

        mockMvc.perform(post("/api/auth/google/onboarding/candidate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"onboardingToken\":\"" + RAW_TOKEN + "\",\"fullName\":\"Test User\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.message", containsString("đã được sử dụng")));
    }
}

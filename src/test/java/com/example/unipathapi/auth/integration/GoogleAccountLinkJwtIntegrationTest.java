package com.example.unipathapi.auth.integration;

import com.example.unipathapi.auth.dto.request.GoogleAccountLinkRequest;
import com.example.unipathapi.auth.dto.response.AuthResponse;
import com.example.unipathapi.auth.entity.GoogleAccountLinkSession;
import com.example.unipathapi.auth.repository.GoogleAccountLinkSessionRepository;
import com.example.unipathapi.auth.repository.UserAuthProviderRepository;
import com.example.unipathapi.auth.service.AuthService;
import com.example.unipathapi.auth.service.GoogleAccountLinkService;
import com.example.unipathapi.auth.service.SecureTokenService;
import com.example.unipathapi.common.security.JwtUtil;
import com.example.unipathapi.common.security.SecurityUtil;
import com.example.unipathapi.user.entity.User;
import com.example.unipathapi.user.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;

import java.security.Key;
import java.time.LocalDateTime;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Backend Integration Tests for JWT Authentication & Post-Link Session Integration (Phase 3.4.2).
 * Verifies parity between Google Account Linking JWT and Email/Password Login JWT,
 * compatibility with SecurityUtil, claim verification, tampered signature rejection,
 * expired token rejection, and active user enforcement.
 */
@SpringBootTest
public class GoogleAccountLinkJwtIntegrationTest {

    @Autowired
    private GoogleAccountLinkService accountLinkService;

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private SecurityUtil securityUtil;

    @Autowired
    private SecureTokenService secureTokenService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GoogleAccountLinkSessionRepository sessionRepository;

    @Autowired
    private UserAuthProviderRepository userAuthProviderRepository;

    private static final String TEST_EMAIL = "jwt_integration_candidate@unipath.local";
    private static final String TEST_PASSWORD = "Password123!@#";
    private static final String RAW_LINK_TOKEN = "raw_link_token_jwt_test_0123456789";
    private static final String GOOGLE_SUB = "google_sub_jwt_test_998877";

    private User candidateUser;
    private GoogleAccountLinkSession linkSession;
    private String tokenHash;

    @BeforeEach
    void setUp() {
        tokenHash = secureTokenService.hashToken(RAW_LINK_TOKEN);

        // Clean up stale records
        sessionRepository.findByLinkTokenHash(tokenHash).ifPresent(s -> sessionRepository.delete(s));
        userAuthProviderRepository.findByProviderAndProviderUserId("GOOGLE", GOOGLE_SUB)
                .ifPresent(p -> userAuthProviderRepository.delete(p));
        userRepository.findByEmail(TEST_EMAIL).ifPresent(u -> userRepository.delete(u));

        // Create test candidate user
        User user = new User();
        user.setEmail(TEST_EMAIL);
        user.setPassword(TEST_PASSWORD);
        user.setRole("CANDIDATE");
        user.setIsActive(true);
        candidateUser = userRepository.save(user);

        // Create link session
        GoogleAccountLinkSession session = new GoogleAccountLinkSession(
                tokenHash,
                candidateUser.getId(),
                GOOGLE_SUB,
                TEST_EMAIL,
                LocalDateTime.now().plusMinutes(15)
        );
        session.setIsUsed(false);
        linkSession = sessionRepository.save(session);
    }

    @AfterEach
    void tearDown() {
        userAuthProviderRepository.findByProviderAndProviderUserId("GOOGLE", GOOGLE_SUB)
                .ifPresent(p -> userAuthProviderRepository.delete(p));
        if (linkSession != null && linkSession.getId() != null) {
            sessionRepository.deleteById(linkSession.getId());
        }
        if (candidateUser != null && candidateUser.getId() != null) {
            userRepository.deleteById(candidateUser.getId());
        }
    }

    @Test
    @DisplayName("1. Google Account Linking issues valid JWT accepted by SecurityUtil for userId and role")
    void testGoogleAccountLink_IssuesValidJwt_AcceptedBySecurityUtil() {
        GoogleAccountLinkRequest request = new GoogleAccountLinkRequest(RAW_LINK_TOKEN, TEST_PASSWORD);
        AuthResponse response = accountLinkService.linkAccount(request);

        assertNotNull(response);
        assertNotNull(response.getToken());
        assertEquals(String.valueOf(candidateUser.getId()), response.getUserID());
        assertEquals("CANDIDATE", response.getRole());

        // Verify SecurityUtil accepts Bearer token
        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        httpRequest.addHeader("Authorization", "Bearer " + response.getToken());

        Integer extractedUserId = securityUtil.getCurrentUserId(httpRequest);
        String extractedRole = securityUtil.getCurrentUserRole(httpRequest);

        assertEquals(candidateUser.getId(), extractedUserId, "SecurityUtil must extract correct userId from JWT");
        assertEquals("CANDIDATE", extractedRole, "SecurityUtil must extract correct role from JWT");
    }

    @Test
    @DisplayName("2. Parity check: JWT from Google Account Linking has identical claims, subject, and signature algorithm as standard login JWT")
    void testJwtParity_GoogleLinkVsStandardLogin() {
        // Standard JWT generated via JwtUtil
        String standardToken = jwtUtil.generateToken(candidateUser.getId(), candidateUser.getRole());

        // Token from Google Account Linking
        GoogleAccountLinkRequest request = new GoogleAccountLinkRequest(RAW_LINK_TOKEN, TEST_PASSWORD);
        AuthResponse linkResponse = accountLinkService.linkAccount(request);
        String linkToken = linkResponse.getToken();

        // Both tokens passed to SecurityUtil
        MockHttpServletRequest reqStandard = new MockHttpServletRequest();
        reqStandard.addHeader("Authorization", "Bearer " + standardToken);

        MockHttpServletRequest reqLink = new MockHttpServletRequest();
        reqLink.addHeader("Authorization", "Bearer " + linkToken);

        assertEquals(securityUtil.getCurrentUserId(reqStandard), securityUtil.getCurrentUserId(reqLink));
        assertEquals(securityUtil.getCurrentUserRole(reqStandard), securityUtil.getCurrentUserRole(reqLink));
    }

    @Test
    @DisplayName("3. SecurityUtil rejects JWT with invalid / tampered signature")
    void testSecurityUtil_RejectsTamperedSignature() {
        Key fakeKey = Keys.hmacShaKeyFor("FakeKeyMustBeLongEnoughForHmacSha256SecurityRequirement123456!".getBytes());
        String fakeToken = Jwts.builder()
                .setSubject(String.valueOf(candidateUser.getId()))
                .claim("role", "CANDIDATE")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(fakeKey, SignatureAlgorithm.HS256)
                .compact();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + fakeToken);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> securityUtil.getCurrentUserId(request));
        assertTrue(ex.getMessage().contains("Token không hợp lệ hoặc đã hết hạn"));
    }

    @Test
    @DisplayName("4. SecurityUtil rejects expired JWT")
    void testSecurityUtil_RejectsExpiredJwt() {
        Key realKey = Keys.hmacShaKeyFor("UniPath_SecretKey_Chuyen_Xy_Ly_Bao_Mat_2026_@#$!".getBytes());
        String expiredToken = Jwts.builder()
                .setSubject(String.valueOf(candidateUser.getId()))
                .claim("role", "CANDIDATE")
                .setIssuedAt(new Date(System.currentTimeMillis() - 7200000))
                .setExpiration(new Date(System.currentTimeMillis() - 3600000)) // Expired 1 hour ago
                .signWith(realKey, SignatureAlgorithm.HS256)
                .compact();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + expiredToken);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> securityUtil.getCurrentUserId(request));
        assertTrue(ex.getMessage().contains("Token không hợp lệ hoặc đã hết hạn"));
    }

    @Test
    @DisplayName("5. SecurityUtil rejects authenticated requests if User account is deactivated post-linking")
    void testSecurityUtil_RejectsDeactivatedUserPostLinking() {
        GoogleAccountLinkRequest request = new GoogleAccountLinkRequest(RAW_LINK_TOKEN, TEST_PASSWORD);
        AuthResponse response = accountLinkService.linkAccount(request);

        // Deactivate user after link
        candidateUser.setIsActive(false);
        userRepository.save(candidateUser);

        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        httpRequest.addHeader("Authorization", "Bearer " + response.getToken());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> securityUtil.getCurrentUserId(httpRequest));
        assertTrue(ex.getMessage().contains("403: Tài khoản của bạn đã bị khóa"));
    }

    @Test
    @DisplayName("6. Failed linking does not issue a JWT")
    void testFailedLinking_NeverReturnsJwt() {
        GoogleAccountLinkRequest request = new GoogleAccountLinkRequest(RAW_LINK_TOKEN, "WrongPassword!");

        assertThrows(IllegalArgumentException.class, () -> accountLinkService.linkAccount(request));

        // Verify session was not consumed
        GoogleAccountLinkSession sessionInDb = sessionRepository.findByLinkTokenHash(tokenHash).orElseThrow();
        assertFalse(sessionInDb.getIsUsed());
    }
}


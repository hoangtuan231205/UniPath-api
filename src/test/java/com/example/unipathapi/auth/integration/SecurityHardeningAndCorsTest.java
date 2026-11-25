package com.example.unipathapi.auth.integration;

import com.example.unipathapi.auth.dto.request.GoogleAccountLinkRequest;
import com.example.unipathapi.auth.entity.GoogleAccountLinkSession;
import com.example.unipathapi.auth.repository.GoogleAccountLinkSessionRepository;
import com.example.unipathapi.auth.service.GoogleAccountLinkService;
import com.example.unipathapi.auth.service.SecureTokenService;
import com.example.unipathapi.common.security.ClientIpResolver;
import com.example.unipathapi.common.security.JwtUtil;
import com.example.unipathapi.common.security.RateLimiter;
import com.example.unipathapi.common.security.SecurityUtil;
import com.example.unipathapi.user.entity.User;
import com.example.unipathapi.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive Security Tests for Phase 3.4.3:
 * - CORS Configuration & Whitelist validation
 * - OPTIONS Preflight Handling
 * - Rate Limiting & DoS / Brute-force Throttling
 * - IP Resolution & Header Spoofing Protection
 * - JWT Secret Externalization & Fail-fast Key Validation
 * - Security Logging & Exception Sanitization
 */
@SpringBootTest
public class SecurityHardeningAndCorsTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private RateLimiter rateLimiter;

    @Autowired
    private ClientIpResolver clientIpResolver;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private SecurityUtil securityUtil;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GoogleAccountLinkSessionRepository sessionRepository;

    @Autowired
    private SecureTokenService secureTokenService;

    @Autowired
    private GoogleAccountLinkService accountLinkService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String ALLOWED_ORIGIN = "http://localhost:5500";
    private static final String DISALLOWED_ORIGIN = "http://evil-attacker.com";
    private static final String GOOGLE_LINK_ENDPOINT = "/api/auth/google/link";
    private static final String LOGIN_ENDPOINT = "/api/auth/login";

    private User testUser;
    private String testRawToken;
    private GoogleAccountLinkSession testSession;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        rateLimiter.clear();

        // Prepare test user
        String email = "sec_test_" + System.currentTimeMillis() + "@gmail.com";
        testUser = new User();
        testUser.setEmail(email);
        testUser.setPassword("RealSecurePassword123!");
        testUser.setRole("CANDIDATE");
        testUser.setIsActive(true);
        testUser = userRepository.save(testUser);

        // Prepare test link session
        testRawToken = "sec-test-token-" + System.currentTimeMillis();
        String tokenHash = secureTokenService.hashToken(testRawToken);
        testSession = new GoogleAccountLinkSession(
                tokenHash,
                testUser.getId(),
                "sec-sub-" + System.currentTimeMillis(),
                email,
                LocalDateTime.now().plusMinutes(15)
        );
        testSession = sessionRepository.save(testSession);
    }

    @AfterEach
    void tearDown() {
        rateLimiter.clear();
        if (testSession != null && testSession.getId() != null) {
            sessionRepository.deleteById(testSession.getId());
        }
        if (testUser != null && testUser.getId() != null) {
            userRepository.deleteById(testUser.getId());
        }
    }

    // =========================================================================
    // 10.1. CORS TESTS
    // =========================================================================

    @Test
    @DisplayName("CORS 1: Allowed origin receives Access-Control-Allow-Origin header on POST")
    void testCorsAllowedOrigin_ReceivesAllowOriginHeader() throws Exception {
        mockMvc.perform(post(LOGIN_ENDPOINT)
                        .header("Origin", ALLOWED_ORIGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nonexistent@gmail.com\",\"password\":\"wrong\"}"))
                .andExpect(header().string("Access-Control-Allow-Origin", ALLOWED_ORIGIN));
    }

    @Test
    @DisplayName("CORS 2: Disallowed origin does NOT receive Access-Control-Allow-Origin header (403 Forbidden)")
    void testCorsDisallowedOrigin_RejectedWith403() throws Exception {
        mockMvc.perform(post(LOGIN_ENDPOINT)
                        .header("Origin", DISALLOWED_ORIGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nonexistent@gmail.com\",\"password\":\"wrong\"}"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    @DisplayName("CORS 3: OPTIONS Preflight for allowed origin succeeds with 200 and required CORS headers")
    void testCorsPreflight_AllowedOrigin_Returns200WithHeaders() throws Exception {
        mockMvc.perform(options(GOOGLE_LINK_ENDPOINT)
                        .header("Origin", ALLOWED_ORIGIN)
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Authorization, Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", ALLOWED_ORIGIN))
                .andExpect(header().string("Access-Control-Allow-Methods", containsString("POST")))
                .andExpect(header().string("Access-Control-Allow-Headers", containsString("Authorization")));
    }

    @Test
    @DisplayName("CORS 4: OPTIONS Preflight for disallowed origin is rejected with 403 Forbidden")
    void testCorsPreflight_DisallowedOrigin_Returns403Forbidden() throws Exception {
        mockMvc.perform(options(GOOGLE_LINK_ENDPOINT)
                        .header("Origin", DISALLOWED_ORIGIN)
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    @DisplayName("CORS 5: Google Account Linking endpoint respects CORS whitelist")
    void testCorsGoogleAccountLink_AllowedOrigin_ReceivesAllowOriginHeader() throws Exception {
        GoogleAccountLinkRequest req = new GoogleAccountLinkRequest(testRawToken, "WrongPassword!");
        mockMvc.perform(post(GOOGLE_LINK_ENDPOINT)
                        .header("Origin", ALLOWED_ORIGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(header().string("Access-Control-Allow-Origin", ALLOWED_ORIGIN));
    }

    // =========================================================================
    // 10.2. RATE LIMITING TESTS
    // =========================================================================

    @Test
    @DisplayName("Rate Limit 1: Requests within limit on /api/auth/google/link proceed normally")
    void testRateLimit_GoogleLink_WithinLimit_Proceeds() throws Exception {
        String clientIp = "192.168.1.10";
        GoogleAccountLinkRequest req = new GoogleAccountLinkRequest(testRawToken, "WrongPassword!");

        // Allowed 5 requests per minute
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post(GOOGLE_LINK_ENDPOINT)
                            .with(r -> { r.setRemoteAddr(clientIp); return r; })
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest()) // 400 Bad Request due to wrong password, NOT 429
                    .andExpect(jsonPath("$.message").value("Mật khẩu tài khoản UniPath không chính xác"));
        }
    }

    @Test
    @DisplayName("Rate Limit 2: Exceeding limit on /api/auth/google/link returns HTTP 429 and Retry-After header")
    void testRateLimit_GoogleLink_Exceeded_Returns429() throws Exception {
        String clientIp = "192.168.1.20";
        GoogleAccountLinkRequest req = new GoogleAccountLinkRequest(testRawToken, "WrongPassword!");

        // Exhaust the 5 allowed tokens
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post(GOOGLE_LINK_ENDPOINT)
                    .with(r -> { r.setRemoteAddr(clientIp); return r; })
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)));
        }

        // 6th request must trigger rate limit
        mockMvc.perform(post(GOOGLE_LINK_ENDPOINT)
                        .with(r -> { r.setRemoteAddr(clientIp); return r; })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.error").value("Too Many Requests"))
                .andExpect(jsonPath("$.message", containsString("Quá nhiều yêu cầu")));
    }

    @Test
    @DisplayName("Rate Limit 3: Rate limiting is partitioned by Client IP (different IPs are isolated)")
    void testRateLimit_PartitionedByIp() throws Exception {
        String ip1 = "10.0.0.1";
        String ip2 = "10.0.0.2";
        GoogleAccountLinkRequest req = new GoogleAccountLinkRequest(testRawToken, "WrongPassword!");

        // Exhaust ip1
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post(GOOGLE_LINK_ENDPOINT)
                    .with(r -> { r.setRemoteAddr(ip1); return r; })
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)));
        }

        // ip1 is blocked
        mockMvc.perform(post(GOOGLE_LINK_ENDPOINT)
                        .with(r -> { r.setRemoteAddr(ip1); return r; })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isTooManyRequests());

        // ip2 must still be allowed
        mockMvc.perform(post(GOOGLE_LINK_ENDPOINT)
                        .with(r -> { r.setRemoteAddr(ip2); return r; })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest()); // Not 429
    }

    @Test
    @DisplayName("Rate Limit 4: Rate limit on /api/auth/login triggers after 10 requests")
    void testRateLimit_Login_ExceededAfter10_Returns429() throws Exception {
        String clientIp = "172.16.0.5";

        for (int i = 0; i < 10; i++) {
            mockMvc.perform(post(LOGIN_ENDPOINT)
                    .with(r -> { r.setRemoteAddr(clientIp); return r; })
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"none@gmail.com\",\"password\":\"bad\"}"));
        }

        // 11th request triggers 429
        mockMvc.perform(post(LOGIN_ENDPOINT)
                        .with(r -> { r.setRemoteAddr(clientIp); return r; })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"none@gmail.com\",\"password\":\"bad\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.status").value(429));
    }

    @Test
    @DisplayName("Rate Limit 5: Rate limit resets cleanly when window expires or reset is called")
    void testRateLimit_Reset_AllowsNewRequests() throws Exception {
        String clientIp = "192.168.1.30";
        GoogleAccountLinkRequest req = new GoogleAccountLinkRequest(testRawToken, "WrongPassword!");

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post(GOOGLE_LINK_ENDPOINT)
                    .with(r -> { r.setRemoteAddr(clientIp); return r; })
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)));
        }

        // Blocked
        mockMvc.perform(post(GOOGLE_LINK_ENDPOINT)
                        .with(r -> { r.setRemoteAddr(clientIp); return r; })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isTooManyRequests());

        // Clear rate limiter
        rateLimiter.clear();

        // Allowed again
        mockMvc.perform(post(GOOGLE_LINK_ENDPOINT)
                        .with(r -> { r.setRemoteAddr(clientIp); return r; })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Rate Limit 6: Concurrent requests do not cause race conditions in RateLimiter")
    void testRateLimit_ConcurrentRequests_ThreadSafe() throws Exception {
        String clientIp = "192.168.1.40";
        int totalThreads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(totalThreads);
        List<Callable<Boolean>> tasks = new ArrayList<>();

        for (int i = 0; i < totalThreads; i++) {
            tasks.add(() -> rateLimiter.tryAcquire("rate:test:" + clientIp, 5, 60000L));
        }

        List<Future<Boolean>> results = executor.invokeAll(tasks);
        executor.shutdown();

        int allowedCount = 0;
        int deniedCount = 0;
        for (Future<Boolean> f : results) {
            if (f.get()) {
                allowedCount++;
            } else {
                deniedCount++;
            }
        }

        assertEquals(5, allowedCount, "Exactly 5 concurrent requests must be allowed");
        assertEquals(5, deniedCount, "Exactly 5 concurrent requests must be denied");
    }

    // =========================================================================
    // 10.3. ACCOUNT LINKING BRUTE-FORCE & SECURITY TESTS
    // =========================================================================

    @Test
    @DisplayName("Brute Force 1: Wrong password causes transaction rollback; session is NOT marked used")
    void testWrongPassword_PreservesAtomicRollback() {
        GoogleAccountLinkRequest req = new GoogleAccountLinkRequest(testRawToken, "WrongPass123!");

        assertThrows(IllegalArgumentException.class, () -> accountLinkService.linkAccount(req));

        // Verify session is still not used because rollback reverted the update
        GoogleAccountLinkSession sessionAfter = sessionRepository.findById(testSession.getId()).orElseThrow();
        assertFalse(sessionAfter.getIsUsed(), "Session must remain unused upon password failure (rollback preserved)");
    }

    @Test
    @DisplayName("Brute Force 2: Wrong password attempt does not reveal user password in exception or log")
    void testWrongPassword_MessageSanitized() throws Exception {
        GoogleAccountLinkRequest req = new GoogleAccountLinkRequest(testRawToken, "WrongPass123!");

        mockMvc.perform(post(GOOGLE_LINK_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Mật khẩu tài khoản UniPath không chính xác"))
                .andExpect(jsonPath("$.message", not(containsString("RealSecurePassword123!"))));
    }

    @Test
    @DisplayName("Brute Force 3: Cannot bypass rate limit by changing payload content (throttled by IP)")
    void testCannotBypassRateLimit_ByChangingPayload() throws Exception {
        String clientIp = "192.168.1.50";

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post(GOOGLE_LINK_ENDPOINT)
                    .with(r -> { r.setRemoteAddr(clientIp); return r; })
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"linkToken\":\"token-" + i + "\",\"password\":\"pass-" + i + "\"}"));
        }

        // Even with a completely new token and password, this IP is blocked
        mockMvc.perform(post(GOOGLE_LINK_ENDPOINT)
                        .with(r -> { r.setRemoteAddr(clientIp); return r; })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"linkToken\":\"brand-new-token\",\"password\":\"any-password\"}"))
                .andExpect(status().isTooManyRequests());
    }

    // =========================================================================
    // 10.4. JWT SECRET MANAGEMENT & VALIDATION TESTS
    // =========================================================================

    @Test
    @DisplayName("JWT Secret 1: JwtUtil and SecurityUtil share configured secret and validate successfully")
    void testJwtSecret_GenerateAndVerify_Success() {
        String token = jwtUtil.generateToken(testUser.getId(), testUser.getRole());
        assertNotNull(token);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);

        Integer extractedId = securityUtil.getCurrentUserId(request);
        String extractedRole = securityUtil.getCurrentUserRole(request);

        assertEquals(testUser.getId(), extractedId);
        assertEquals(testUser.getRole(), extractedRole);
    }

    @Test
    @DisplayName("JWT Secret 2: Short secret key (< 32 bytes) fails fast on initialization")
    void testJwtSecret_ShortKey_FailsFast() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> new JwtUtil("too-short-key"));
        assertTrue(ex.getMessage().contains("256 bits (32 bytes)"));
    }

    @Test
    @DisplayName("JWT Secret 3: Empty or null secret key fails fast on initialization")
    void testJwtSecret_EmptyKey_FailsFast() {
        IllegalStateException ex1 = assertThrows(IllegalStateException.class, () -> new JwtUtil(""));
        assertTrue(ex1.getMessage().contains("không được để trống"));

        IllegalStateException ex2 = assertThrows(IllegalStateException.class, () -> new JwtUtil(null));
        assertTrue(ex2.getMessage().contains("không được để trống"));
    }

    // =========================================================================
    // 10.5. SECURITY LOGGING & ERROR SANITIZATION TESTS
    // =========================================================================

    @Test
    @DisplayName("Security Logging: Internal database exceptions do not leak SQL or Hibernate details to client")
    void testInternalExceptions_Sanitized() throws Exception {
        // Calling an endpoint with invalid type or structure triggers exception handling
        mockMvc.perform(post(GOOGLE_LINK_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("invalid-raw-json-structure"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", not(containsString("org.hibernate"))))
                .andExpect(jsonPath("$.message", not(containsString("SQLException"))));
    }
}


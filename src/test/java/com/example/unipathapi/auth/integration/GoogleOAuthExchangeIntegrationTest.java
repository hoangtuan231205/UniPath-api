package com.example.unipathapi.auth.integration;

import com.example.unipathapi.auth.dto.request.GoogleAuthRequest;
import com.example.unipathapi.common.security.RateLimiter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Spring Boot Integration Tests for Google OAuth Authorization Code Exchange Endpoint (Phase 3.5).
 * Tests CORS preflight, Rate Limiting, DTO Validation, and Error Sanitization.
 */
@SpringBootTest
public class GoogleOAuthExchangeIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private RateLimiter rateLimiter;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private static final String ENDPOINT = "/api/auth/google";
    private static final String ALLOWED_ORIGIN = "http://localhost:5500";

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        rateLimiter.clear();
    }

    @AfterEach
    void tearDown() {
        rateLimiter.clear();
    }

    @Test
    @DisplayName("1. CORS Preflight OPTIONS /api/auth/google returns 200 OK with allowed headers")
    void testCorsPreflight_Success() throws Exception {
        mockMvc.perform(options(ENDPOINT)
                        .header("Origin", ALLOWED_ORIGIN)
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "content-type,x-requested-with"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", ALLOWED_ORIGIN))
                .andExpect(header().string("Access-Control-Allow-Methods", containsString("POST")));
    }

    @Test
    @DisplayName("2. POST /api/auth/google Rate Limiter throttles on excessive attempts with HTTP 429")
    void testRateLimiter_ThrottlesExcessiveRequests() throws Exception {
        GoogleAuthRequest request = new GoogleAuthRequest("invalid-dummy-code");
        String content = objectMapper.writeValueAsString(request);
        String clientIp = "192.168.10.55";

        // Endpoint allows 10 requests per 60 seconds
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(post(ENDPOINT)
                            .with(req -> {
                                req.setRemoteAddr(clientIp);
                                return req;
                            })
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(content))
                    // Could be 400 (invalid code) or 503 (Google unreachable), but NOT 429
                    .andExpect(status().is(anyOf(is(400), is(503))));
        }

        // 11th request must be rejected by RateLimitInterceptor with 429 Too Many Requests
        mockMvc.perform(post(ENDPOINT)
                        .with(req -> {
                            req.setRemoteAddr(clientIp);
                            return req;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(content))
                .andExpect(status().is(429))
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.status", is(429)))
                .andExpect(jsonPath("$.error", is("Too Many Requests")))
                .andExpect(jsonPath("$.message", containsString("Quá nhiều yêu cầu")));
    }

    @Test
    @DisplayName("3. POST /api/auth/google rejects empty JSON with 400 Bad Request")
    void testEmptyPayload_Returns400() throws Exception {
        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.authorizationCode", notNullValue()));
    }
}


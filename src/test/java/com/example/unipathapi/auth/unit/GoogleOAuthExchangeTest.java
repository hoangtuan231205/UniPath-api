package com.example.unipathapi.auth.unit;

import com.example.unipathapi.auth.controller.GoogleAccountLinkController;
import com.example.unipathapi.auth.dto.GoogleAuthResult;
import com.example.unipathapi.auth.dto.request.GoogleAuthRequest;
import com.example.unipathapi.auth.dto.response.AuthResponse;
import com.example.unipathapi.auth.exception.GoogleAuthException;
import com.example.unipathapi.auth.exception.GoogleServiceUnavailableException;
import com.example.unipathapi.auth.exception.InvalidAuthorizationCodeException;
import com.example.unipathapi.auth.service.GoogleAccountLinkService;
import com.example.unipathapi.auth.service.GoogleAuthService;
import com.example.unipathapi.auth.service.GoogleOAuthSecurityValidator;
import com.example.unipathapi.common.exception.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit & MockMvc Tests for Google OAuth Authorization Code Exchange (Phase 3.5).
 * Tests endpoint POST /api/auth/google.
 */
@ExtendWith(MockitoExtension.class)
public class GoogleOAuthExchangeTest {

    private MockMvc mockMvc;

    @Mock
    private GoogleAccountLinkService accountLinkService;

    @Mock
    private GoogleAuthService googleAuthService;

    @Mock
    private GoogleOAuthSecurityValidator securityValidator;

    @InjectMocks
    private GoogleAccountLinkController googleAuthController;

    private ObjectMapper objectMapper;

    private static final String ENDPOINT = "/api/auth/google";
    private static final String VALID_CODE = "4/0AeanS0b-mock-authorization-code-1234567890";
    private static final String ALLOWED_ORIGIN = "http://localhost:5500";
    private static final String VALID_X_REQUESTED_WITH = "XmlHttpRequest";

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(googleAuthController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("1. POST /api/auth/google - Success Flow A (AUTHENTICATED) returns JWT and user role")
    void testAuthenticate_Success_Authenticated() throws Exception {
        GoogleAuthRequest request = new GoogleAuthRequest(VALID_CODE);
        AuthResponse authResponse = new AuthResponse("mock.jwt.token", "101", "CANDIDATE", "Đăng nhập bằng Google thành công!");
        GoogleAuthResult result = GoogleAuthResult.authenticated(authResponse, 101, "candidate@unipath.vn", "CANDIDATE");

        when(googleAuthService.authenticateWithGoogleCode(VALID_CODE)).thenReturn(result);

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Origin", ALLOWED_ORIGIN)
                        .header("X-Requested-With", VALID_X_REQUESTED_WITH)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("AUTHENTICATED")))
                .andExpect(jsonPath("$.userId", is(101)))
                .andExpect(jsonPath("$.email", is("candidate@unipath.vn")))
                .andExpect(jsonPath("$.role", is("CANDIDATE")))
                .andExpect(jsonPath("$.authResponse.token", is("mock.jwt.token")))
                .andExpect(jsonPath("$.authResponse.userID", is("101")))
                .andExpect(jsonPath("$.authResponse.userId", is("101")))
                .andExpect(jsonPath("$.authResponse.role", is("CANDIDATE")));

        verify(googleAuthService, times(1)).authenticateWithGoogleCode(VALID_CODE);
        verify(securityValidator, times(1)).validatePopupHeaders(ALLOWED_ORIGIN, VALID_X_REQUESTED_WITH);
    }

    @Test
    @DisplayName("2. POST /api/auth/google - Flow B1 (ACCOUNT_LINK_REQUIRED) returns linkToken and target email")
    void testAuthenticate_Success_AccountLinkRequired() throws Exception {
        GoogleAuthRequest request = new GoogleAuthRequest(VALID_CODE);
        GoogleAuthResult result = GoogleAuthResult.accountLinkRequired(
                "mock-link-token-xyz-123",
                "existing.user@unipath.vn",
                LocalDateTime.now().plusMinutes(15)
        );

        when(googleAuthService.authenticateWithGoogleCode(VALID_CODE)).thenReturn(result);

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ACCOUNT_LINK_REQUIRED")))
                .andExpect(jsonPath("$.linkToken", is("mock-link-token-xyz-123")))
                .andExpect(jsonPath("$.email", is("existing.user@unipath.vn")))
                // Ensure no UniPath JWT token is issued in this flow
                .andExpect(jsonPath("$.authResponse").doesNotExist())
                .andExpect(jsonPath("$.token").doesNotExist());

        verify(googleAuthService, times(1)).authenticateWithGoogleCode(VALID_CODE);
    }

    @Test
    @DisplayName("3. POST /api/auth/google - Flow B2 (ONBOARDING_REQUIRED) returns onboardingToken and Google profile")
    void testAuthenticate_Success_OnboardingRequired() throws Exception {
        GoogleAuthRequest request = new GoogleAuthRequest(VALID_CODE);
        GoogleAuthResult result = GoogleAuthResult.onboardingRequired(
                "mock-onboarding-token-456",
                "new.student@gmail.com",
                "Nguyen Van A",
                "https://lh3.googleusercontent.com/a/photo.jpg",
                LocalDateTime.now().plusMinutes(15)
        );

        when(googleAuthService.authenticateWithGoogleCode(VALID_CODE)).thenReturn(result);

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ONBOARDING_REQUIRED")))
                .andExpect(jsonPath("$.onboardingToken", is("mock-onboarding-token-456")))
                .andExpect(jsonPath("$.email", is("new.student@gmail.com")))
                .andExpect(jsonPath("$.name", is("Nguyen Van A")))
                .andExpect(jsonPath("$.picture", containsString("photo.jpg")));

        verify(googleAuthService, times(1)).authenticateWithGoogleCode(VALID_CODE);
    }

    @Test
    @DisplayName("4. POST /api/auth/google - Missing authorizationCode returns 400 Bad Request")
    void testAuthenticate_MissingCode_Returns400() throws Exception {
        String emptyJson = "{}";

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(emptyJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.authorizationCode", notNullValue()));

        verify(googleAuthService, never()).authenticateWithGoogleCode(anyString());
    }

    @Test
    @DisplayName("5. POST /api/auth/google - Blank authorizationCode returns 400 Bad Request")
    void testAuthenticate_BlankCode_Returns400() throws Exception {
        String blankJson = "{\"authorizationCode\": \"   \"}";

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(blankJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.authorizationCode", notNullValue()));

        verify(googleAuthService, never()).authenticateWithGoogleCode(anyString());
    }

    @Test
    @DisplayName("6. POST /api/auth/google - Json alias 'code' is accepted as authorizationCode")
    void testAuthenticate_AliasCode_Success() throws Exception {
        String aliasJson = "{\"code\": \"" + VALID_CODE + "\"}";
        AuthResponse authResponse = new AuthResponse("mock.jwt.token", "50", "EMPLOYER", "Đăng nhập thành công!");
        GoogleAuthResult result = GoogleAuthResult.authenticated(authResponse, 50, "hr@company.com", "EMPLOYER");

        when(googleAuthService.authenticateWithGoogleCode(VALID_CODE)).thenReturn(result);

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(aliasJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("AUTHENTICATED")))
                .andExpect(jsonPath("$.role", is("EMPLOYER")));

        verify(googleAuthService, times(1)).authenticateWithGoogleCode(VALID_CODE);
    }

    @Test
    @DisplayName("7. POST /api/auth/google - CSRF check: Missing X-Requested-With with Origin returns 400 Bad Request")
    void testAuthenticate_MissingXRequestedWith_Rejected() throws Exception {
        GoogleAuthRequest request = new GoogleAuthRequest(VALID_CODE);

        doThrow(new GoogleAuthException("Missing or invalid X-Requested-With header; request rejected by CSRF protection"))
                .when(securityValidator).validatePopupHeaders(eq(ALLOWED_ORIGIN), isNull());

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Origin", ALLOWED_ORIGIN)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("CSRF protection")));

        verify(googleAuthService, never()).authenticateWithGoogleCode(anyString());
    }

    @Test
    @DisplayName("8. POST /api/auth/google - Invalid or expired authorization code returns 400 Bad Request")
    void testAuthenticate_InvalidAuthorizationCode_Returns400() throws Exception {
        GoogleAuthRequest request = new GoogleAuthRequest("invalid-or-used-code");

        when(googleAuthService.authenticateWithGoogleCode("invalid-or-used-code"))
                .thenThrow(new InvalidAuthorizationCodeException("Google authorization code is invalid or has expired"));

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("invalid or has expired")));
    }

    @Test
    @DisplayName("9. POST /api/auth/google - Google service unavailable returns 503 Service Unavailable")
    void testAuthenticate_GoogleServiceUnavailable_Returns503() throws Exception {
        GoogleAuthRequest request = new GoogleAuthRequest(VALID_CODE);

        when(googleAuthService.authenticateWithGoogleCode(VALID_CODE))
                .thenThrow(new GoogleServiceUnavailableException("Google token service is temporarily unreachable"));

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status", is(503)))
                .andExpect(jsonPath("$.message", containsString("temporarily unreachable")));
    }

    @Test
    @DisplayName("10. POST /api/auth/google - Locked account returns 403 Forbidden")
    void testAuthenticate_DeactivatedAccount_Returns403() throws Exception {
        GoogleAuthRequest request = new GoogleAuthRequest(VALID_CODE);

        when(googleAuthService.authenticateWithGoogleCode(VALID_CODE))
                .thenThrow(new GoogleAuthException("Tài khoản của bạn đã bị khoá. Vui lòng liên hệ quản trị viên."));

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.message", containsString("khoá")));
    }

    @Test
    @DisplayName("11. POST /api/auth/google/ - Trailing slash endpoint maps identically")
    void testAuthenticate_TrailingSlash_Success() throws Exception {
        GoogleAuthRequest request = new GoogleAuthRequest(VALID_CODE);
        AuthResponse authResponse = new AuthResponse("mock.jwt.token", "200", "CANDIDATE", "Đăng nhập thành công!");
        GoogleAuthResult result = GoogleAuthResult.authenticated(authResponse, 200, "user@unipath.vn", "CANDIDATE");

        when(googleAuthService.authenticateWithGoogleCode(VALID_CODE)).thenReturn(result);

        mockMvc.perform(post(ENDPOINT + "/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("AUTHENTICATED")));
    }

    @Test
    @DisplayName("12. Security Verification - Response never leaks Google client secret or internal credentials")
    void testSecurity_NoSensitiveCredentialsLeaked() throws Exception {
        GoogleAuthRequest request = new GoogleAuthRequest(VALID_CODE);
        AuthResponse authResponse = new AuthResponse("mock.jwt.token", "1", "CANDIDATE", "Thành công");
        GoogleAuthResult result = GoogleAuthResult.authenticated(authResponse, 1, "student@unipath.vn", "CANDIDATE");

        when(googleAuthService.authenticateWithGoogleCode(VALID_CODE)).thenReturn(result);

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientSecret").doesNotExist())
                .andExpect(jsonPath("$.client_secret").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.authorizationCode").doesNotExist());
    }
}


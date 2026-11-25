package com.example.unipathapi.auth.unit;

import com.example.unipathapi.auth.controller.GoogleAccountLinkController;
import com.example.unipathapi.auth.dto.request.GoogleAccountLinkRequest;
import com.example.unipathapi.auth.dto.response.AuthResponse;
import com.example.unipathapi.auth.service.GoogleAccountLinkService;
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

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller Unit and MockMvc Tests for GoogleAccountLinkController (Phase 3.4.1).
 * Verifies API contract, DTO validation, HTTP status mapping, security boundaries, and exception handling.
 */
@ExtendWith(MockitoExtension.class)
public class GoogleAccountLinkControllerTest {

    private MockMvc mockMvc;

    @Mock
    private GoogleAccountLinkService accountLinkService;

    @InjectMocks
    private GoogleAccountLinkController accountLinkController;

    private ObjectMapper objectMapper;

    private static final String ENDPOINT = "/api/auth/google/link";
    private static final String VALID_TOKEN = "valid-link-token-sample-1234567890";
    private static final String VALID_PASSWORD = "ValidSecurePassword123!";

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(accountLinkController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("1. POST /api/auth/google/link - Returns 200 OK and AuthResponse on valid request")
    void testLinkAccount_Success_Returns200AndAuthResponse() throws Exception {
        GoogleAccountLinkRequest request = new GoogleAccountLinkRequest(VALID_TOKEN, VALID_PASSWORD);
        AuthResponse mockResponse = new AuthResponse("mock.jwt.token", "42", "CANDIDATE", "Liên kết tài khoản Google thành công!");

        when(accountLinkService.linkAccount(any(GoogleAccountLinkRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", is("mock.jwt.token")))
                .andExpect(jsonPath("$.userID", is("42")))
                .andExpect(jsonPath("$.role", is("CANDIDATE")))
                .andExpect(jsonPath("$.message", containsString("thành công")))
                // Security verification: no sensitive fields leaked in response
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.googleSub").doesNotExist())
                .andExpect(jsonPath("$.linkToken").doesNotExist());

        verify(accountLinkService, times(1)).linkAccount(any(GoogleAccountLinkRequest.class));
    }

    @Test
    @DisplayName("2. POST /api/auth/google/link - Service is called exactly once with exact request fields")
    void testLinkAccount_CallsServiceExactlyOnce() throws Exception {
        GoogleAccountLinkRequest request = new GoogleAccountLinkRequest(VALID_TOKEN, VALID_PASSWORD);
        AuthResponse mockResponse = new AuthResponse("mock.jwt.token", "100", "EMPLOYER", "Thành công");

        when(accountLinkService.linkAccount(any(GoogleAccountLinkRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(accountLinkService, times(1)).linkAccount(argThat(req ->
                VALID_TOKEN.equals(req.getLinkToken()) && VALID_PASSWORD.equals(req.getPassword())
        ));
    }

    @Test
    @DisplayName("3. POST /api/auth/google/link - Missing linkToken returns 400 Bad Request with validation error")
    void testLinkAccount_MissingLinkToken_Returns400() throws Exception {
        String json = "{\"password\": \"" + VALID_PASSWORD + "\"}";

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Dữ liệu không hợp lệ")))
                .andExpect(jsonPath("$.errors.linkToken", containsString("không được để trống")));

        verifyNoInteractions(accountLinkService);
    }

    @Test
    @DisplayName("4. POST /api/auth/google/link - Blank linkToken returns 400 Bad Request")
    void testLinkAccount_BlankLinkToken_Returns400() throws Exception {
        GoogleAccountLinkRequest request = new GoogleAccountLinkRequest("   ", VALID_PASSWORD);

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.errors.linkToken", containsString("không được để trống")));

        verifyNoInteractions(accountLinkService);
    }

    @Test
    @DisplayName("5. POST /api/auth/google/link - Missing password returns 400 Bad Request with validation error")
    void testLinkAccount_MissingPassword_Returns400() throws Exception {
        String json = "{\"linkToken\": \"" + VALID_TOKEN + "\"}";

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.errors.password", containsString("không được để trống")));

        verifyNoInteractions(accountLinkService);
    }

    @Test
    @DisplayName("6. POST /api/auth/google/link - Blank password returns 400 Bad Request")
    void testLinkAccount_BlankPassword_Returns400() throws Exception {
        GoogleAccountLinkRequest request = new GoogleAccountLinkRequest(VALID_TOKEN, "   ");

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.errors.password", containsString("không được để trống")));

        verifyNoInteractions(accountLinkService);
    }

    @Test
    @DisplayName("7. POST /api/auth/google/link - Empty request body returns 400 Bad Request")
    void testLinkAccount_EmptyBody_Returns400() throws Exception {
        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("không đúng định dạng")));

        verifyNoInteractions(accountLinkService);
    }

    @Test
    @DisplayName("8. POST /api/auth/google/link - Service throws IllegalArgumentException (wrong password) -> 400 Bad Request")
    void testLinkAccount_WrongPassword_Returns400() throws Exception {
        GoogleAccountLinkRequest request = new GoogleAccountLinkRequest(VALID_TOKEN, "WrongPassword!");

        when(accountLinkService.linkAccount(any(GoogleAccountLinkRequest.class)))
                .thenThrow(new IllegalArgumentException("Mật khẩu tài khoản UniPath không chính xác"));

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", is("Mật khẩu tài khoản UniPath không chính xác")));
    }

    @Test
    @DisplayName("9. POST /api/auth/google/link - Service throws IllegalArgumentException (session not found) -> 400 Bad Request")
    void testLinkAccount_SessionNotFound_Returns400() throws Exception {
        GoogleAccountLinkRequest request = new GoogleAccountLinkRequest(VALID_TOKEN, VALID_PASSWORD);

        when(accountLinkService.linkAccount(any(GoogleAccountLinkRequest.class)))
                .thenThrow(new IllegalArgumentException("Phiên liên kết tài khoản không tồn tại hoặc không hợp lệ"));

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", is("Phiên liên kết tài khoản không tồn tại hoặc không hợp lệ")));
    }

    @Test
    @DisplayName("10. POST /api/auth/google/link - Service throws IllegalStateException (session already used) -> 409 Conflict")
    void testLinkAccount_SessionAlreadyUsed_Returns409() throws Exception {
        GoogleAccountLinkRequest request = new GoogleAccountLinkRequest(VALID_TOKEN, VALID_PASSWORD);

        when(accountLinkService.linkAccount(any(GoogleAccountLinkRequest.class)))
                .thenThrow(new IllegalStateException("Phiên liên kết tài khoản đã được sử dụng"));

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.error", is("Conflict")))
                .andExpect(jsonPath("$.message", is("Phiên liên kết tài khoản đã được sử dụng")));
    }

    @Test
    @DisplayName("11. POST /api/auth/google/link - Service throws IllegalStateException (session expired) -> 409 Conflict")
    void testLinkAccount_SessionExpired_Returns409() throws Exception {
        GoogleAccountLinkRequest request = new GoogleAccountLinkRequest(VALID_TOKEN, VALID_PASSWORD);

        when(accountLinkService.linkAccount(any(GoogleAccountLinkRequest.class)))
                .thenThrow(new IllegalStateException("Phiên liên kết tài khoản đã hết hạn"));

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.message", is("Phiên liên kết tài khoản đã hết hạn")));
    }

    @Test
    @DisplayName("12. POST /api/auth/google/link - Service throws IllegalStateException (Google sub already linked) -> 409 Conflict")
    void testLinkAccount_GoogleSubAlreadyLinked_Returns409() throws Exception {
        GoogleAccountLinkRequest request = new GoogleAccountLinkRequest(VALID_TOKEN, VALID_PASSWORD);

        when(accountLinkService.linkAccount(any(GoogleAccountLinkRequest.class)))
                .thenThrow(new IllegalStateException("Tài khoản Google này đã được liên kết với một tài khoản khác"));

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.message", is("Tài khoản Google này đã được liên kết với một tài khoản khác")));
    }

    @Test
    @DisplayName("13. POST /api/auth/google/link - Service throws IllegalStateException (User already has Google provider) -> 409 Conflict")
    void testLinkAccount_UserAlreadyHasGoogleProvider_Returns409() throws Exception {
        GoogleAccountLinkRequest request = new GoogleAccountLinkRequest(VALID_TOKEN, VALID_PASSWORD);

        when(accountLinkService.linkAccount(any(GoogleAccountLinkRequest.class)))
                .thenThrow(new IllegalStateException("Tài khoản UniPath này đã được liên kết với tài khoản Google"));

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.message", is("Tài khoản UniPath này đã được liên kết với tài khoản Google")));
    }

    @Test
    @DisplayName("14. POST /api/auth/google/link - Inactive / locked account returns 403 Forbidden")
    void testLinkAccount_LockedAccount_Returns403() throws Exception {
        GoogleAccountLinkRequest request = new GoogleAccountLinkRequest(VALID_TOKEN, VALID_PASSWORD);

        when(accountLinkService.linkAccount(any(GoogleAccountLinkRequest.class)))
                .thenThrow(new IllegalArgumentException("Tài khoản của bạn đã bị khoá. Vui lòng liên hệ quản trị viên."));

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.error", is("Forbidden")))
                .andExpect(jsonPath("$.message", containsString("khoá")));
    }

    @Test
    @DisplayName("15. POST /api/auth/google/link - Unexpected exception returns 500 Internal Server Error without leaking details")
    void testLinkAccount_UnexpectedException_Returns500() throws Exception {
        GoogleAccountLinkRequest request = new GoogleAccountLinkRequest(VALID_TOKEN, VALID_PASSWORD);

        when(accountLinkService.linkAccount(any(GoogleAccountLinkRequest.class)))
                .thenAnswer(inv -> { throw new Exception("Unexpected critical system crash"); });

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status", is(500)))
                .andExpect(jsonPath("$.error", is("Lỗi máy chủ")));
    }

    @Test
    @DisplayName("16. POST /api/auth/google/link - Identity Boundary: Client cannot inject userId or googleSub")
    void testLinkAccount_IdentityBoundary_ClientCannotInjectIdentity() throws Exception {
        // Attempting to pass userId or googleSub in JSON payload
        String maliciousPayload = "{" +
                "\"linkToken\": \"" + VALID_TOKEN + "\"," +
                "\"password\": \"" + VALID_PASSWORD + "\"," +
                "\"userId\": 9999," +
                "\"googleSub\": \"hacked-google-sub\"," +
                "\"role\": \"ADMIN\"" +
                "}";

        AuthResponse mockResponse = new AuthResponse("mock.jwt.token", "42", "CANDIDATE", "Thành công");
        when(accountLinkService.linkAccount(any(GoogleAccountLinkRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(maliciousPayload))
                .andExpect(status().isOk());

        // Verifies the service received only the DTO fields (linkToken, password)
        verify(accountLinkService, times(1)).linkAccount(argThat(req ->
                VALID_TOKEN.equals(req.getLinkToken()) && VALID_PASSWORD.equals(req.getPassword())
        ));
    }
}


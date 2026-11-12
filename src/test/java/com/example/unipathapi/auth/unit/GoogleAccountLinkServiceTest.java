package com.example.unipathapi.auth.unit;

import com.example.unipathapi.auth.dto.request.GoogleAccountLinkRequest;
import com.example.unipathapi.auth.dto.response.AuthResponse;
import com.example.unipathapi.auth.entity.GoogleAccountLinkSession;
import com.example.unipathapi.auth.entity.UserAuthProvider;
import com.example.unipathapi.auth.repository.GoogleAccountLinkSessionRepository;
import com.example.unipathapi.auth.repository.UserAuthProviderRepository;
import com.example.unipathapi.auth.service.GoogleAccountLinkService;
import com.example.unipathapi.auth.service.SecureTokenService;
import com.example.unipathapi.common.security.JwtUtil;
import com.example.unipathapi.user.entity.User;
import com.example.unipathapi.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GoogleAccountLinkServiceTest {

    @Mock
    private GoogleAccountLinkSessionRepository accountLinkSessionRepository;

    @Mock
    private UserAuthProviderRepository userAuthProviderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecureTokenService secureTokenService;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private GoogleAccountLinkService accountLinkService;

    private static final String RAW_TOKEN = "raw-link-token-xyz-12345678901234567890";
    private static final String TOKEN_HASH = "mock-sha256-hash-64characterslong00000000000000000000000000000000000";
    private static final String GOOGLE_SUB = "google-sub-9988776655";
    private static final String EMAIL = "student@university.edu.vn";
    private static final String CORRECT_PASSWORD = "SecretPassword123!";

    private User sampleUser;
    private GoogleAccountLinkSession sampleSession;

    @BeforeEach
    void setUp() {
        sampleUser = new User();
        sampleUser.setId(42);
        sampleUser.setEmail(EMAIL);
        sampleUser.setPassword(CORRECT_PASSWORD);
        sampleUser.setRole("CANDIDATE");
        sampleUser.setIsActive(true);

        sampleSession = new GoogleAccountLinkSession(
                TOKEN_HASH,
                42,
                GOOGLE_SUB,
                EMAIL,
                LocalDateTime.now().plusMinutes(15)
        );
        sampleSession.setId(101);
        sampleSession.setIsUsed(false);
    }

    @Test
    @DisplayName("1. linkAccount succeeds with valid linkToken and correct password")
    void testLinkAccount_Success() {
        GoogleAccountLinkRequest request = new GoogleAccountLinkRequest(RAW_TOKEN, CORRECT_PASSWORD);

        when(secureTokenService.hashToken(RAW_TOKEN)).thenReturn(TOKEN_HASH);
        when(accountLinkSessionRepository.findByLinkTokenHash(TOKEN_HASH)).thenReturn(Optional.of(sampleSession));
        when(accountLinkSessionRepository.consumeSession(eq(TOKEN_HASH), any(LocalDateTime.class))).thenReturn(1);
        when(userRepository.findById(42)).thenReturn(Optional.of(sampleUser));
        when(userAuthProviderRepository.existsByProviderAndProviderUserId("GOOGLE", GOOGLE_SUB)).thenReturn(false);
        when(userAuthProviderRepository.existsByUserIdAndProvider(42, "GOOGLE")).thenReturn(false);
        when(jwtUtil.generateToken(42, "CANDIDATE")).thenReturn("mock.jwt.token");

        AuthResponse response = accountLinkService.linkAccount(request);

        assertNotNull(response);
        assertEquals("mock.jwt.token", response.getToken());
        assertEquals("42", response.getUserID());
        assertEquals("CANDIDATE", response.getRole());
        assertTrue(response.getMessage().contains("thành công"));

        // Verify UserAuthProvider saved
        ArgumentCaptor<UserAuthProvider> providerCaptor = ArgumentCaptor.forClass(UserAuthProvider.class);
        verify(userAuthProviderRepository, times(1)).saveAndFlush(providerCaptor.capture());
        UserAuthProvider saved = providerCaptor.getValue();
        assertEquals("GOOGLE", saved.getProvider());
        assertEquals(GOOGLE_SUB, saved.getProviderUserId());
        assertEquals(EMAIL, saved.getEmail());
        assertEquals(sampleUser, saved.getUser());

        // Verify residual sessions invalidated
        verify(accountLinkSessionRepository, times(1)).invalidateUnusedSessionsByGoogleSub(GOOGLE_SUB);
    }

    @Test
    @DisplayName("2. linkAccount throws IllegalArgumentException when request is null")
    void testLinkAccount_NullRequest() {
        assertThrows(IllegalArgumentException.class, () -> accountLinkService.linkAccount(null));
        verifyNoInteractions(secureTokenService, accountLinkSessionRepository, userRepository, userAuthProviderRepository);
    }

    @Test
    @DisplayName("3. linkAccount throws IllegalArgumentException when linkToken is null or blank")
    void testLinkAccount_BlankLinkToken() {
        GoogleAccountLinkRequest request1 = new GoogleAccountLinkRequest(null, CORRECT_PASSWORD);
        GoogleAccountLinkRequest request2 = new GoogleAccountLinkRequest("   ", CORRECT_PASSWORD);

        assertThrows(IllegalArgumentException.class, () -> accountLinkService.linkAccount(request1));
        assertThrows(IllegalArgumentException.class, () -> accountLinkService.linkAccount(request2));
        verifyNoInteractions(accountLinkSessionRepository);
    }

    @Test
    @DisplayName("4. linkAccount throws IllegalArgumentException when password is null or empty")
    void testLinkAccount_BlankPassword() {
        GoogleAccountLinkRequest request1 = new GoogleAccountLinkRequest(RAW_TOKEN, null);
        GoogleAccountLinkRequest request2 = new GoogleAccountLinkRequest(RAW_TOKEN, "");

        assertThrows(IllegalArgumentException.class, () -> accountLinkService.linkAccount(request1));
        assertThrows(IllegalArgumentException.class, () -> accountLinkService.linkAccount(request2));
        verifyNoInteractions(accountLinkSessionRepository);
    }

    @Test
    @DisplayName("5. linkAccount throws IllegalArgumentException when tokenHash is not found in database")
    void testLinkAccount_SessionNotFound() {
        GoogleAccountLinkRequest request = new GoogleAccountLinkRequest(RAW_TOKEN, CORRECT_PASSWORD);
        when(secureTokenService.hashToken(RAW_TOKEN)).thenReturn(TOKEN_HASH);
        when(accountLinkSessionRepository.findByLinkTokenHash(TOKEN_HASH)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> accountLinkService.linkAccount(request));
        assertTrue(ex.getMessage().contains("không tồn tại hoặc không hợp lệ"));
        verify(accountLinkSessionRepository, never()).consumeSession(anyString(), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("6. linkAccount throws IllegalStateException when session is already used")
    void testLinkAccount_SessionAlreadyUsed() {
        sampleSession.setIsUsed(true);
        GoogleAccountLinkRequest request = new GoogleAccountLinkRequest(RAW_TOKEN, CORRECT_PASSWORD);

        when(secureTokenService.hashToken(RAW_TOKEN)).thenReturn(TOKEN_HASH);
        when(accountLinkSessionRepository.findByLinkTokenHash(TOKEN_HASH)).thenReturn(Optional.of(sampleSession));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> accountLinkService.linkAccount(request));
        assertTrue(ex.getMessage().contains("đã được sử dụng"));
        verify(accountLinkSessionRepository, never()).consumeSession(anyString(), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("7. linkAccount throws IllegalStateException when session has expired")
    void testLinkAccount_SessionExpired() {
        sampleSession.setExpiresAt(LocalDateTime.now().minusMinutes(5));
        GoogleAccountLinkRequest request = new GoogleAccountLinkRequest(RAW_TOKEN, CORRECT_PASSWORD);

        when(secureTokenService.hashToken(RAW_TOKEN)).thenReturn(TOKEN_HASH);
        when(accountLinkSessionRepository.findByLinkTokenHash(TOKEN_HASH)).thenReturn(Optional.of(sampleSession));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> accountLinkService.linkAccount(request));
        assertTrue(ex.getMessage().contains("đã hết hạn"));
        verify(accountLinkSessionRepository, never()).consumeSession(anyString(), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("8. linkAccount throws IllegalStateException when atomic consumeSession updates 0 rows")
    void testLinkAccount_AtomicConsumeRaceCondition() {
        GoogleAccountLinkRequest request = new GoogleAccountLinkRequest(RAW_TOKEN, CORRECT_PASSWORD);

        when(secureTokenService.hashToken(RAW_TOKEN)).thenReturn(TOKEN_HASH);
        when(accountLinkSessionRepository.findByLinkTokenHash(TOKEN_HASH)).thenReturn(Optional.of(sampleSession));
        when(accountLinkSessionRepository.consumeSession(eq(TOKEN_HASH), any(LocalDateTime.class))).thenReturn(0);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> accountLinkService.linkAccount(request));
        assertTrue(ex.getMessage().contains("đã được sử dụng hoặc đã hết hạn"));
        verify(userRepository, never()).findById(anyInt());
    }

    @Test
    @DisplayName("9. linkAccount throws IllegalArgumentException when User is not found in database")
    void testLinkAccount_UserNotFound() {
        GoogleAccountLinkRequest request = new GoogleAccountLinkRequest(RAW_TOKEN, CORRECT_PASSWORD);

        when(secureTokenService.hashToken(RAW_TOKEN)).thenReturn(TOKEN_HASH);
        when(accountLinkSessionRepository.findByLinkTokenHash(TOKEN_HASH)).thenReturn(Optional.of(sampleSession));
        when(accountLinkSessionRepository.consumeSession(eq(TOKEN_HASH), any(LocalDateTime.class))).thenReturn(1);
        when(userRepository.findById(42)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> accountLinkService.linkAccount(request));
        assertTrue(ex.getMessage().contains("Tài khoản người dùng không tồn tại"));
        verify(userAuthProviderRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("10. linkAccount throws IllegalArgumentException when User is inactive / deactivated")
    void testLinkAccount_UserInactive() {
        sampleUser.setIsActive(false);
        GoogleAccountLinkRequest request = new GoogleAccountLinkRequest(RAW_TOKEN, CORRECT_PASSWORD);

        when(secureTokenService.hashToken(RAW_TOKEN)).thenReturn(TOKEN_HASH);
        when(accountLinkSessionRepository.findByLinkTokenHash(TOKEN_HASH)).thenReturn(Optional.of(sampleSession));
        when(accountLinkSessionRepository.consumeSession(eq(TOKEN_HASH), any(LocalDateTime.class))).thenReturn(1);
        when(userRepository.findById(42)).thenReturn(Optional.of(sampleUser));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> accountLinkService.linkAccount(request));
        assertTrue(ex.getMessage().contains("Tài khoản của bạn đã bị khoá"));
        verify(userAuthProviderRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("11. linkAccount throws IllegalArgumentException when password does not match")
    void testLinkAccount_WrongPassword() {
        GoogleAccountLinkRequest request = new GoogleAccountLinkRequest(RAW_TOKEN, "WrongPassword123");

        when(secureTokenService.hashToken(RAW_TOKEN)).thenReturn(TOKEN_HASH);
        when(accountLinkSessionRepository.findByLinkTokenHash(TOKEN_HASH)).thenReturn(Optional.of(sampleSession));
        when(accountLinkSessionRepository.consumeSession(eq(TOKEN_HASH), any(LocalDateTime.class))).thenReturn(1);
        when(userRepository.findById(42)).thenReturn(Optional.of(sampleUser));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> accountLinkService.linkAccount(request));
        assertTrue(ex.getMessage().contains("Mật khẩu tài khoản UniPath không chính xác"));
        verify(userAuthProviderRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("12. linkAccount throws IllegalArgumentException when User has null password (Google-only account)")
    void testLinkAccount_NullPassword() {
        sampleUser.setPassword(null);
        GoogleAccountLinkRequest request = new GoogleAccountLinkRequest(RAW_TOKEN, CORRECT_PASSWORD);

        when(secureTokenService.hashToken(RAW_TOKEN)).thenReturn(TOKEN_HASH);
        when(accountLinkSessionRepository.findByLinkTokenHash(TOKEN_HASH)).thenReturn(Optional.of(sampleSession));
        when(accountLinkSessionRepository.consumeSession(eq(TOKEN_HASH), any(LocalDateTime.class))).thenReturn(1);
        when(userRepository.findById(42)).thenReturn(Optional.of(sampleUser));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> accountLinkService.linkAccount(request));
        assertTrue(ex.getMessage().contains("Mật khẩu tài khoản UniPath không chính xác"));
        verify(userAuthProviderRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("13. linkAccount throws IllegalStateException when Google sub is already linked to another account")
    void testLinkAccount_GoogleSubAlreadyLinked() {
        GoogleAccountLinkRequest request = new GoogleAccountLinkRequest(RAW_TOKEN, CORRECT_PASSWORD);

        when(secureTokenService.hashToken(RAW_TOKEN)).thenReturn(TOKEN_HASH);
        when(accountLinkSessionRepository.findByLinkTokenHash(TOKEN_HASH)).thenReturn(Optional.of(sampleSession));
        when(accountLinkSessionRepository.consumeSession(eq(TOKEN_HASH), any(LocalDateTime.class))).thenReturn(1);
        when(userRepository.findById(42)).thenReturn(Optional.of(sampleUser));
        when(userAuthProviderRepository.existsByProviderAndProviderUserId("GOOGLE", GOOGLE_SUB)).thenReturn(true);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> accountLinkService.linkAccount(request));
        assertTrue(ex.getMessage().contains("Tài khoản Google này đã được liên kết với một tài khoản khác"));
        verify(userAuthProviderRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("14. linkAccount throws IllegalStateException when User already has a Google provider linked")
    void testLinkAccount_UserAlreadyHasGoogleProvider() {
        GoogleAccountLinkRequest request = new GoogleAccountLinkRequest(RAW_TOKEN, CORRECT_PASSWORD);

        when(secureTokenService.hashToken(RAW_TOKEN)).thenReturn(TOKEN_HASH);
        when(accountLinkSessionRepository.findByLinkTokenHash(TOKEN_HASH)).thenReturn(Optional.of(sampleSession));
        when(accountLinkSessionRepository.consumeSession(eq(TOKEN_HASH), any(LocalDateTime.class))).thenReturn(1);
        when(userRepository.findById(42)).thenReturn(Optional.of(sampleUser));
        when(userAuthProviderRepository.existsByProviderAndProviderUserId("GOOGLE", GOOGLE_SUB)).thenReturn(false);
        when(userAuthProviderRepository.existsByUserIdAndProvider(42, "GOOGLE")).thenReturn(true);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> accountLinkService.linkAccount(request));
        assertTrue(ex.getMessage().contains("Tài khoản UniPath này đã được liên kết với tài khoản Google"));
        verify(userAuthProviderRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("15. linkAccount trims rawLinkToken before hashing")
    void testLinkAccount_TrimsRawToken() {
        GoogleAccountLinkRequest request = new GoogleAccountLinkRequest("   " + RAW_TOKEN + "   ", CORRECT_PASSWORD);

        when(secureTokenService.hashToken(RAW_TOKEN)).thenReturn(TOKEN_HASH);
        when(accountLinkSessionRepository.findByLinkTokenHash(TOKEN_HASH)).thenReturn(Optional.of(sampleSession));
        when(accountLinkSessionRepository.consumeSession(eq(TOKEN_HASH), any(LocalDateTime.class))).thenReturn(1);
        when(userRepository.findById(42)).thenReturn(Optional.of(sampleUser));
        when(userAuthProviderRepository.existsByProviderAndProviderUserId("GOOGLE", GOOGLE_SUB)).thenReturn(false);
        when(userAuthProviderRepository.existsByUserIdAndProvider(42, "GOOGLE")).thenReturn(false);
        when(jwtUtil.generateToken(42, "CANDIDATE")).thenReturn("mock.jwt.token");

        AuthResponse response = accountLinkService.linkAccount(request);
        assertNotNull(response);
        verify(secureTokenService, times(1)).hashToken(RAW_TOKEN);
    }

    @Test
    @DisplayName("16. JWT is never issued when password verification fails")
    void testLinkAccount_JwtNeverIssuedOnWrongPassword() {
        GoogleAccountLinkRequest request = new GoogleAccountLinkRequest(RAW_TOKEN, "WrongPassword123");

        when(secureTokenService.hashToken(RAW_TOKEN)).thenReturn(TOKEN_HASH);
        when(accountLinkSessionRepository.findByLinkTokenHash(TOKEN_HASH)).thenReturn(Optional.of(sampleSession));
        when(accountLinkSessionRepository.consumeSession(eq(TOKEN_HASH), any(LocalDateTime.class))).thenReturn(1);
        when(userRepository.findById(42)).thenReturn(Optional.of(sampleUser));

        assertThrows(IllegalArgumentException.class, () -> accountLinkService.linkAccount(request));
        verifyNoInteractions(jwtUtil);
    }

    @Test
    @DisplayName("17. JWT is never issued when user account is inactive")
    void testLinkAccount_JwtNeverIssuedOnInactiveUser() {
        sampleUser.setIsActive(false);
        GoogleAccountLinkRequest request = new GoogleAccountLinkRequest(RAW_TOKEN, CORRECT_PASSWORD);

        when(secureTokenService.hashToken(RAW_TOKEN)).thenReturn(TOKEN_HASH);
        when(accountLinkSessionRepository.findByLinkTokenHash(TOKEN_HASH)).thenReturn(Optional.of(sampleSession));
        when(accountLinkSessionRepository.consumeSession(eq(TOKEN_HASH), any(LocalDateTime.class))).thenReturn(1);
        when(userRepository.findById(42)).thenReturn(Optional.of(sampleUser));

        assertThrows(IllegalArgumentException.class, () -> accountLinkService.linkAccount(request));
        verifyNoInteractions(jwtUtil);
    }

    @Test
    @DisplayName("18. JWT is never issued when Google identity is already linked")
    void testLinkAccount_JwtNeverIssuedOnGoogleSubAlreadyLinked() {
        GoogleAccountLinkRequest request = new GoogleAccountLinkRequest(RAW_TOKEN, CORRECT_PASSWORD);

        when(secureTokenService.hashToken(RAW_TOKEN)).thenReturn(TOKEN_HASH);
        when(accountLinkSessionRepository.findByLinkTokenHash(TOKEN_HASH)).thenReturn(Optional.of(sampleSession));
        when(accountLinkSessionRepository.consumeSession(eq(TOKEN_HASH), any(LocalDateTime.class))).thenReturn(1);
        when(userRepository.findById(42)).thenReturn(Optional.of(sampleUser));
        when(userAuthProviderRepository.existsByProviderAndProviderUserId("GOOGLE", GOOGLE_SUB)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> accountLinkService.linkAccount(request));
        verifyNoInteractions(jwtUtil);
    }

    @Test
    @DisplayName("19. JWT is never issued when User already has Google provider linked")
    void testLinkAccount_JwtNeverIssuedOnUserAlreadyHasGoogleProvider() {
        GoogleAccountLinkRequest request = new GoogleAccountLinkRequest(RAW_TOKEN, CORRECT_PASSWORD);

        when(secureTokenService.hashToken(RAW_TOKEN)).thenReturn(TOKEN_HASH);
        when(accountLinkSessionRepository.findByLinkTokenHash(TOKEN_HASH)).thenReturn(Optional.of(sampleSession));
        when(accountLinkSessionRepository.consumeSession(eq(TOKEN_HASH), any(LocalDateTime.class))).thenReturn(1);
        when(userRepository.findById(42)).thenReturn(Optional.of(sampleUser));
        when(userAuthProviderRepository.existsByProviderAndProviderUserId("GOOGLE", GOOGLE_SUB)).thenReturn(false);
        when(userAuthProviderRepository.existsByUserIdAndProvider(42, "GOOGLE")).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> accountLinkService.linkAccount(request));
        verifyNoInteractions(jwtUtil);
    }

    @Test
    @DisplayName("20. Persistence exception on saveAndFlush throws IllegalStateException and never issues JWT or invalidates sessions")
    void testLinkAccount_SaveThrowsDataIntegrityViolation_ThrowsIllegalStateExceptionAndNoJwt() {
        GoogleAccountLinkRequest request = new GoogleAccountLinkRequest(RAW_TOKEN, CORRECT_PASSWORD);

        when(secureTokenService.hashToken(RAW_TOKEN)).thenReturn(TOKEN_HASH);
        when(accountLinkSessionRepository.findByLinkTokenHash(TOKEN_HASH)).thenReturn(Optional.of(sampleSession));
        when(accountLinkSessionRepository.consumeSession(eq(TOKEN_HASH), any(LocalDateTime.class))).thenReturn(1);
        when(userRepository.findById(42)).thenReturn(Optional.of(sampleUser));
        when(userAuthProviderRepository.existsByProviderAndProviderUserId("GOOGLE", GOOGLE_SUB)).thenReturn(false);
        when(userAuthProviderRepository.existsByUserIdAndProvider(42, "GOOGLE")).thenReturn(false);
        doThrow(new DataIntegrityViolationException("duplicate key error"))
                .when(userAuthProviderRepository).saveAndFlush(any(UserAuthProvider.class));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> accountLinkService.linkAccount(request));
        assertTrue(ex.getMessage().contains("Tài khoản Google này hoặc tài khoản UniPath đã được liên kết"));
        verifyNoInteractions(jwtUtil);
        verify(accountLinkSessionRepository, never()).invalidateUnusedSessionsByGoogleSub(anyString());
    }

    @Test
    @DisplayName("21. Constant-time password verification correctly matches Unicode & special character passwords")
    void testLinkAccount_ConstantTimeComparison_HandlesSpecialCharacters() {
        String complexPassword = "MậtKhẩu_Siêu_BảoMật_2026!@#$%^&*()_+~`";
        sampleUser.setPassword(complexPassword);
        GoogleAccountLinkRequest request = new GoogleAccountLinkRequest(RAW_TOKEN, complexPassword);

        when(secureTokenService.hashToken(RAW_TOKEN)).thenReturn(TOKEN_HASH);
        when(accountLinkSessionRepository.findByLinkTokenHash(TOKEN_HASH)).thenReturn(Optional.of(sampleSession));
        when(accountLinkSessionRepository.consumeSession(eq(TOKEN_HASH), any(LocalDateTime.class))).thenReturn(1);
        when(userRepository.findById(42)).thenReturn(Optional.of(sampleUser));
        when(userAuthProviderRepository.existsByProviderAndProviderUserId("GOOGLE", GOOGLE_SUB)).thenReturn(false);
        when(userAuthProviderRepository.existsByUserIdAndProvider(42, "GOOGLE")).thenReturn(false);
        when(jwtUtil.generateToken(42, "CANDIDATE")).thenReturn("mock.jwt.token");

        AuthResponse response = accountLinkService.linkAccount(request);
        assertNotNull(response);
        assertEquals("mock.jwt.token", response.getToken());
    }
}

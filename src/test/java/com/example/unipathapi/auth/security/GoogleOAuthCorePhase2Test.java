package com.example.unipathapi.auth.security;

import com.example.unipathapi.auth.dto.GoogleTokenPayload;
import com.example.unipathapi.auth.exception.GoogleAuthException;
import com.example.unipathapi.auth.exception.GoogleServiceUnavailableException;
import com.example.unipathapi.auth.exception.InvalidAuthorizationCodeException;
import com.example.unipathapi.auth.exception.InvalidGoogleTokenException;
import com.example.unipathapi.auth.service.GoogleJwksKeyProvider;
import com.example.unipathapi.auth.service.GoogleOAuthSecurityValidator;
import com.example.unipathapi.auth.service.GoogleTokenVerifierService;
import com.example.unipathapi.auth.service.SecureTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import com.example.unipathapi.auth.dto.request.CandidateRegisterRequest;
import com.example.unipathapi.auth.dto.request.EmployerRegisterRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Duration;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

/**
 * Phase 2 Unit Tests: Google OAuth Core & Token Security.
 * Timestamp: 17:02:15.
 *
 * All tests use pure cryptographic operations (real RSA-2048 keypairs and SHA-256)
 * and MockRestServiceServer for HTTP endpoints, ensuring 100% offline and deterministic execution.
 */
public class GoogleOAuthCorePhase2Test {

    private static final String CLIENT_ID = "unipath-test-client.apps.googleusercontent.com";
    private static final String CLIENT_SECRET = "unipath-test-secret-value";
    private static final String TOKEN_URI = "https://oauth2.googleapis.com/token";
    private static final String JWKS_URI = "https://www.googleapis.com/oauth2/v3/certs";
    private static final String REDIRECT_URI = "postmessage";

    private static KeyPair keyPair1;
    private static KeyPair keyPair2;

    private GoogleJwksKeyProvider jwksKeyProvider;
    private GoogleTokenVerifierService tokenVerifierService;
    private SecureTokenService secureTokenService;
    private RestTemplate restTemplate;
    private MockRestServiceServer mockServer;
    private ObjectMapper objectMapper;

    @BeforeAll
    public static void setupKeyPairs() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        keyPair1 = kpg.generateKeyPair();
        keyPair2 = kpg.generateKeyPair();
    }

    private GoogleOAuthSecurityValidator securityValidator;

    @BeforeEach
    public void setup() {
        objectMapper = new ObjectMapper();
        restTemplate = new RestTemplate();
        mockServer = MockRestServiceServer.bindTo(restTemplate).ignoreExpectOrder(true).build();

        jwksKeyProvider = new GoogleJwksKeyProvider(JWKS_URI, restTemplate, objectMapper);
        jwksKeyProvider.registerKey("kid-1", keyPair1.getPublic());

        tokenVerifierService = new GoogleTokenVerifierService(
                CLIENT_ID,
                CLIENT_SECRET,
                TOKEN_URI,
                REDIRECT_URI,
                jwksKeyProvider,
                restTemplate,
                objectMapper
        );

        secureTokenService = new SecureTokenService();
        securityValidator = new GoogleOAuthSecurityValidator("http://localhost:5500,http://127.0.0.1:5500");
    }

    private String buildJwt(KeyPair kp, String kid, String issuer, String audience,
                            Date exp, String sub, String email, Boolean emailVerified,
                            String name, String picture) {
        return buildJwtCustom(kp, kid, issuer, audience, exp, sub, email, emailVerified, null, name, picture);
    }

    private String buildJwtCustom(KeyPair kp, String kid, String issuer, String audience,
                                  Date exp, String sub, Object email, Object emailVerified,
                                  String azp, String name, String picture) {
        var builder = Jwts.builder()
                .setHeaderParam("kid", kid)
                .setHeaderParam("alg", "RS256")
                .setSubject(sub)
                .setIssuer(issuer)
                .setAudience(audience)
                .setIssuedAt(new Date(System.currentTimeMillis() - 1000))
                .setExpiration(exp);

        if (email != null) builder.claim("email", email);
        if (emailVerified != null) builder.claim("email_verified", emailVerified);
        if (azp != null) builder.claim("azp", azp);
        if (name != null) builder.claim("name", name);
        if (picture != null) builder.claim("picture", picture);

        return builder.signWith(kp.getPrivate(), SignatureAlgorithm.RS256).compact();
    }

    // 1. Google ID Token hợp lệ
    @Test
    public void testValidIdToken_Success() {
        Date exp = new Date(System.currentTimeMillis() + 3600000);
        String token = buildJwt(keyPair1, "kid-1", "https://accounts.google.com", CLIENT_ID,
                exp, "google-sub-12345", "student@gmail.com", true, "Nguyen Van A", "https://avatar.url/photo.jpg");

        GoogleTokenPayload payload = tokenVerifierService.verifyIdToken(token);

        assertNotNull(payload);
        assertEquals("google-sub-12345", payload.getSub());
        assertEquals("student@gmail.com", payload.getEmail());
        assertEquals("Nguyen Van A", payload.getName());
        assertEquals("https://avatar.url/photo.jpg", payload.getPicture());
        assertTrue(payload.isEmailVerified());
    }

    // 2. Chữ ký sai (ký bằng private key khác)
    @Test
    public void testInvalidSignature_ThrowsException() {
        Date exp = new Date(System.currentTimeMillis() + 3600000);
        // Signed with keyPair2, but kid header specifies "kid-1" whose registered public key is from keyPair1
        String token = buildJwt(keyPair2, "kid-1", "https://accounts.google.com", CLIENT_ID,
                exp, "google-sub-12345", "student@gmail.com", true, "Nguyen Van A", null);

        InvalidGoogleTokenException ex = assertThrows(InvalidGoogleTokenException.class, () ->
                tokenVerifierService.verifyIdToken(token)
        );
        assertTrue(ex.getMessage().contains("signature verification failed"));
    }

    // 3. Issuer sai
    @Test
    public void testWrongIssuer_ThrowsException() {
        Date exp = new Date(System.currentTimeMillis() + 3600000);
        String token = buildJwt(keyPair1, "kid-1", "https://malicious-issuer.com", CLIENT_ID,
                exp, "google-sub-12345", "student@gmail.com", true, "Nguyen Van A", null);

        InvalidGoogleTokenException ex = assertThrows(InvalidGoogleTokenException.class, () ->
                tokenVerifierService.verifyIdToken(token)
        );
        assertTrue(ex.getMessage().contains("Invalid Google token issuer"));
    }

    // 4. Audience sai
    @Test
    public void testWrongAudience_ThrowsException() {
        Date exp = new Date(System.currentTimeMillis() + 3600000);
        String token = buildJwt(keyPair1, "kid-1", "https://accounts.google.com", "foreign-client-id",
                exp, "google-sub-12345", "student@gmail.com", true, "Nguyen Van A", null);

        InvalidGoogleTokenException ex = assertThrows(InvalidGoogleTokenException.class, () ->
                tokenVerifierService.verifyIdToken(token)
        );
        assertTrue(ex.getMessage().contains("audience mismatch"));
    }

    // 5. Token hết hạn
    @Test
    public void testExpiredToken_ThrowsException() {
        Date expiredTime = new Date(System.currentTimeMillis() - 10000); // 10s in past
        String token = buildJwt(keyPair1, "kid-1", "https://accounts.google.com", CLIENT_ID,
                expiredTime, "google-sub-12345", "student@gmail.com", true, "Nguyen Van A", null);

        InvalidGoogleTokenException ex = assertThrows(InvalidGoogleTokenException.class, () ->
                tokenVerifierService.verifyIdToken(token)
        );
        assertTrue(ex.getMessage().contains("expired"));
    }

    // 6. Email chưa verified
    @Test
    public void testUnverifiedEmail_ThrowsException() {
        Date exp = new Date(System.currentTimeMillis() + 3600000);
        String token = buildJwt(keyPair1, "kid-1", "https://accounts.google.com", CLIENT_ID,
                exp, "google-sub-12345", "student@gmail.com", false, "Nguyen Van A", null);

        InvalidGoogleTokenException ex = assertThrows(InvalidGoogleTokenException.class, () ->
                tokenVerifierService.verifyIdToken(token)
        );
        assertTrue(ex.getMessage().contains("not verified"));
    }

    // 7. Thiếu Google sub
    @Test
    public void testMissingSub_ThrowsException() {
        Date exp = new Date(System.currentTimeMillis() + 3600000);
        String token = buildJwt(keyPair1, "kid-1", "https://accounts.google.com", CLIENT_ID,
                exp, "", "student@gmail.com", true, "Nguyen Van A", null);

        InvalidGoogleTokenException ex = assertThrows(InvalidGoogleTokenException.class, () ->
                tokenVerifierService.verifyIdToken(token)
        );
        assertTrue(ex.getMessage().contains("missing subject"));
    }

    // 8. Thiếu Email
    @Test
    public void testMissingEmail_ThrowsException() {
        Date exp = new Date(System.currentTimeMillis() + 3600000);
        String token = buildJwt(keyPair1, "kid-1", "https://accounts.google.com", CLIENT_ID,
                exp, "google-sub-12345", null, true, "Nguyen Van A", null);

        InvalidGoogleTokenException ex = assertThrows(InvalidGoogleTokenException.class, () ->
                tokenVerifierService.verifyIdToken(token)
        );
        assertTrue(ex.getMessage().contains("missing email"));
    }

    // 9. Key rotation: Khi gặp kid mới, provider gọi refresh và xác minh thành công
    @Test
    public void testKeyRotation_Success() throws Exception {
        jwksKeyProvider.resetCooldown();
        java.security.interfaces.RSAPublicKey rsaPub = (java.security.interfaces.RSAPublicKey) keyPair2.getPublic();
        byte[] modBytes = rsaPub.getModulus().toByteArray();
        if (modBytes.length > 1 && modBytes[0] == 0) {
            byte[] unsigned = new byte[modBytes.length - 1];
            System.arraycopy(modBytes, 1, unsigned, 0, unsigned.length);
            modBytes = unsigned;
        }
        String nBase64 = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(modBytes);
        String eBase64 = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(rsaPub.getPublicExponent().toByteArray());

        String jwksResponse = String.format("""
                {
                  "keys": [
                    {
                      "kty": "RSA",
                      "alg": "RS256",
                      "use": "sig",
                      "kid": "kid-rotated",
                      "n": "%s",
                      "e": "%s"
                    }
                  ]
                }
                """, nBase64, eBase64);

        mockServer.expect(requestTo(JWKS_URI))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(jwksResponse, MediaType.APPLICATION_JSON));

        // Token signed with rotated key
        Date exp = new Date(System.currentTimeMillis() + 3600000);
        String rotatedToken = buildJwt(keyPair2, "kid-rotated", "https://accounts.google.com", CLIENT_ID,
                exp, "google-sub-rotated", "user.rotated@gmail.com", true, "Rotated User", null);

        GoogleTokenPayload payload = tokenVerifierService.verifyIdToken(rotatedToken);

        assertNotNull(payload);
        assertEquals("google-sub-rotated", payload.getSub());
        assertEquals("user.rotated@gmail.com", payload.getEmail());
    }

    // 10. JWKS endpoint không truy cập được khi không có key
    @Test
    public void testJwksUnavailable_ThrowsException() {
        jwksKeyProvider.clearCache();
        mockServer.expect(requestTo(JWKS_URI))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withServerError());

        Date exp = new Date(System.currentTimeMillis() + 3600000);
        String token = buildJwt(keyPair1, "kid-unknown", "https://accounts.google.com", CLIENT_ID,
                exp, "google-sub-12345", "user@gmail.com", true, null, null);

        assertThrows(GoogleServiceUnavailableException.class, () ->
                tokenVerifierService.verifyIdToken(token)
        );
    }

    // 11. Trao đổi authorization code thành công
    @Test
    public void testExchangeCode_Success() {
        Date exp = new Date(System.currentTimeMillis() + 3600000);
        String sampleIdToken = buildJwt(keyPair1, "kid-1", "https://accounts.google.com", CLIENT_ID,
                exp, "sub-exchange-success", "authcode.user@gmail.com", true, "Auth Code User", null);

        String tokenResponseJson = String.format("""
                {
                  "access_token": "ya29.sample_google_access_token_never_logged",
                  "expires_in": 3599,
                  "id_token": "%s",
                  "token_type": "Bearer",
                  "scope": "openid email profile"
                }
                """, sampleIdToken);

        mockServer.expect(requestTo(TOKEN_URI))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andRespond(withSuccess(tokenResponseJson, MediaType.APPLICATION_JSON));

        GoogleTokenPayload payload = tokenVerifierService.exchangeCodeAndVerify("4/0ASVsf8_sample_auth_code");

        assertNotNull(payload);
        assertEquals("sub-exchange-success", payload.getSub());
        assertEquals("authcode.user@gmail.com", payload.getEmail());
        assertEquals("Auth Code User", payload.getName());
    }

    // 12. Trao đổi authorization code thất bại (invalid_grant từ Google)
    @Test
    public void testExchangeCode_InvalidGrant_ThrowsException() {
        String errorJson = """
                {
                  "error": "invalid_grant",
                  "error_description": "Bad Request"
                }
                """;

        mockServer.expect(requestTo(TOKEN_URI))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withBadRequest().body(errorJson).contentType(MediaType.APPLICATION_JSON));

        InvalidAuthorizationCodeException ex = assertThrows(InvalidAuthorizationCodeException.class, () ->
                tokenVerifierService.exchangeCodeForIdToken("invalid_or_expired_code")
        );
        assertTrue(ex.getMessage().contains("invalid or has expired"));
    }

    // 13. SecureTokenService: sinh token 32 byte entropy, Base64URL không padding (43 ký tự)
    @Test
    public void testSecureTokenGeneration_EntropyAndFormat() {
        String token1 = secureTokenService.generateSecureToken();
        String token2 = secureTokenService.generateSecureToken();

        assertNotNull(token1);
        assertNotNull(token2);
        assertNotEquals(token1, token2);

        // 32 bytes in Base64 produces 43 characters when padding is omitted (ceil(32 * 4 / 3) = 43)
        assertEquals(43, token1.length(), "Token length must be 43 characters (32 bytes Base64URL without padding)");
        assertEquals(43, token2.length());

        // Must not contain padding character '=' or standard Base64 characters '+' or '/'
        assertFalse(token1.contains("="), "Token must not have padding");
        assertFalse(token1.contains("+"), "Token must be URL-safe (no '+')");
        assertFalse(token1.contains("/"), "Token must be URL-safe (no '/')");
    }

    // 14. SecureTokenService: SHA-256 hash chuẩn 64 ký tự hex và có tính tất định
    @Test
    public void testSecureTokenHash_DeterministicAnd64Hex() {
        String rawToken = "sample_test_token_for_sha256_verification_12345";
        String hash1 = secureTokenService.hashToken(rawToken);
        String hash2 = secureTokenService.hashToken(rawToken);

        assertNotNull(hash1);
        assertEquals(64, hash1.length(), "SHA-256 hex digest must be exactly 64 characters");
        assertEquals(hash1, hash2, "Hashing same token must be deterministic");
        assertTrue(hash1.matches("^[0-9a-f]{64}$"), "Hash must be lowercase hexadecimal");
    }

    // 15. SecureTokenService: Phát hiện sửa đổi (tamper detection)
    @Test
    public void testSecureTokenHash_TamperDetection() {
        String tokenOriginal = secureTokenService.generateSecureToken();
        String tokenTampered = tokenOriginal.substring(0, tokenOriginal.length() - 1) + "X";

        String hashOriginal = secureTokenService.hashToken(tokenOriginal);
        String hashTampered = secureTokenService.hashToken(tokenTampered);

        assertNotEquals(hashOriginal, hashTampered, "Tampered token must produce completely different hash");
    }

    // 16. SecureTokenService: Từ chối token rỗng
    @Test
    public void testSecureTokenHash_BlankRejection() {
        assertThrows(IllegalArgumentException.class, () -> secureTokenService.hashToken(null));
        assertThrows(IllegalArgumentException.class, () -> secureTokenService.hashToken("   "));
    }

    // 17. Security Audit: aud sai bị từ chối dù azp trùng khớp (ngăn chặn bypass bằng azp)
    @Test
    public void testAudienceMismatchEvenWithMatchingAzp_ThrowsException() {
        Date exp = new Date(System.currentTimeMillis() + 3600000);
        String token = buildJwtCustom(keyPair1, "kid-1", "https://accounts.google.com",
                "malicious-other-app.apps.googleusercontent.com",
                exp, "google-sub-12345", "user@gmail.com", true,
                CLIENT_ID, "Nguyen Van A", null);

        InvalidGoogleTokenException ex = assertThrows(InvalidGoogleTokenException.class, () ->
                tokenVerifierService.verifyIdToken(token)
        );
        assertTrue(ex.getMessage().contains("audience mismatch"), "Must reject token whose aud doesn't match client ID");
    }

    // 18. Security Audit: azp sai bị từ chối dù aud trùng khớp
    @Test
    public void testAzpMismatchEvenWithMatchingAudience_ThrowsException() {
        Date exp = new Date(System.currentTimeMillis() + 3600000);
        String token = buildJwtCustom(keyPair1, "kid-1", "https://accounts.google.com",
                CLIENT_ID,
                exp, "google-sub-12345", "user@gmail.com", true,
                "attacker-app.apps.googleusercontent.com", "Nguyen Van A", null);

        InvalidGoogleTokenException ex = assertThrows(InvalidGoogleTokenException.class, () ->
                tokenVerifierService.verifyIdToken(token)
        );
        assertTrue(ex.getMessage().contains("authorized party (azp) mismatch"), "Must reject token with mismatched azp");
    }

    // 19. Security Audit: email_verified dạng String 'true' bị từ chối (bắt buộc kiểu boolean)
    @Test
    public void testEmailVerifiedAsString_ThrowsException() {
        Date exp = new Date(System.currentTimeMillis() + 3600000);
        String token = buildJwtCustom(keyPair1, "kid-1", "https://accounts.google.com",
                CLIENT_ID,
                exp, "google-sub-12345", "user@gmail.com", "true", // String instead of Boolean
                null, "Nguyen Van A", null);

        InvalidGoogleTokenException ex = assertThrows(InvalidGoogleTokenException.class, () ->
                tokenVerifierService.verifyIdToken(token)
        );
        assertTrue(ex.getMessage().contains("not verified"), "Must reject non-boolean email_verified");
    }

    // 20. Security Audit: Email sai định dạng (thiếu @ hoặc domain) bị từ chối
    @Test
    public void testEmailInvalidFormat_ThrowsException() {
        Date exp = new Date(System.currentTimeMillis() + 3600000);
        String token = buildJwtCustom(keyPair1, "kid-1", "https://accounts.google.com",
                CLIENT_ID,
                exp, "google-sub-12345", "not-an-email-address", true,
                null, "Nguyen Van A", null);

        InvalidGoogleTokenException ex = assertThrows(InvalidGoogleTokenException.class, () ->
                tokenVerifierService.verifyIdToken(token)
        );
        assertTrue(ex.getMessage().contains("invalid email format"));
    }

    // 21. Security Audit: Chống nhiều request đồng thời cùng refresh JWKS (double-checked locking)
    @Test
    public void testConcurrentJwksRefresh_SingleNetworkCall() throws Exception {
        jwksKeyProvider.resetCooldown();
        java.security.interfaces.RSAPublicKey rsaPub = (java.security.interfaces.RSAPublicKey) keyPair2.getPublic();
        byte[] modBytes = rsaPub.getModulus().toByteArray();
        if (modBytes.length > 1 && modBytes[0] == 0) {
            byte[] unsigned = new byte[modBytes.length - 1];
            System.arraycopy(modBytes, 1, unsigned, 0, unsigned.length);
            modBytes = unsigned;
        }
        String nBase64 = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(modBytes);
        String eBase64 = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(rsaPub.getPublicExponent().toByteArray());

        String jwksResponse = String.format("""
                {
                  "keys": [
                    {
                      "kty": "RSA",
                      "alg": "RS256",
                      "use": "sig",
                      "kid": "kid-concurrent",
                      "n": "%s",
                      "e": "%s"
                    }
                  ]
                }
                """, nBase64, eBase64);

        // Expect exactly 1 network request even with multiple concurrent callers
        mockServer.expect(requestTo(JWKS_URI))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(jwksResponse, MediaType.APPLICATION_JSON));

        int threadCount = 4;
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(threadCount);
        java.util.concurrent.CountDownLatch ready = new java.util.concurrent.CountDownLatch(threadCount);
        java.util.concurrent.CountDownLatch start = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.atomic.AtomicInteger successCount = new java.util.concurrent.atomic.AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    java.security.PublicKey pk = jwksKeyProvider.getPublicKey("kid-concurrent");
                    if (pk != null) {
                        successCount.incrementAndGet();
                    }
                } catch (Exception ignored) {
                }
            });
        }

        ready.await();
        start.countDown();
        executor.shutdown();
        executor.awaitTermination(3, java.util.concurrent.TimeUnit.SECONDS);

        assertEquals(threadCount, successCount.get(), "All threads should successfully obtain the key");
    }

    // 22. Security Audit: Khóa cũ quá 24h không được sử dụng khi Google endpoint ngừng hoạt động
    @Test
    public void testStaleKeyOlderThan24Hours_ThrowsException() throws Exception {
        jwksKeyProvider.registerKey("kid-ancient", keyPair1.getPublic());

        // Use reflection to set lastFetchTime to 25 hours ago
        java.lang.reflect.Field field = GoogleJwksKeyProvider.class.getDeclaredField("lastFetchTime");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        java.util.concurrent.atomic.AtomicReference<java.time.Instant> ref =
                (java.util.concurrent.atomic.AtomicReference<java.time.Instant>) field.get(jwksKeyProvider);
        ref.set(java.time.Instant.now().minus(java.time.Duration.ofHours(25)));

        mockServer.expect(requestTo(JWKS_URI))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withServerError());

        Date exp = new Date(System.currentTimeMillis() + 3600000);
        String token = buildJwt(keyPair1, "kid-ancient", "https://accounts.google.com", CLIENT_ID,
                exp, "google-sub-12345", "user@gmail.com", true, "User", null);

        assertThrows(GoogleServiceUnavailableException.class, () ->
                tokenVerifierService.verifyIdToken(token),
                "Keys older than 24h must be rejected if refresh fails"
        );
    }

    // 23. Security Audit: GIS Popup request hợp lệ (có X-Requested-With và đúng Origin)
    @Test
    public void testGoogleSecurityValidator_ValidHeaders_Success() {
        assertDoesNotThrow(() ->
                securityValidator.validatePopupHeaders("http://localhost:5500", "XmlHttpRequest")
        );
        assertDoesNotThrow(() ->
                securityValidator.validatePopupHeaders("http://127.0.0.1:5500", "xmlhttprequest")
        );
    }

    // 24. Security Audit: Thiếu X-Requested-With bị từ chối CSRF
    @Test
    public void testGoogleSecurityValidator_MissingXRequestedWith_ThrowsException() {
        GoogleAuthException ex1 = assertThrows(GoogleAuthException.class, () ->
                securityValidator.validatePopupHeaders("http://localhost:5500", null)
        );
        assertTrue(ex1.getMessage().contains("CSRF protection"));

        GoogleAuthException ex2 = assertThrows(GoogleAuthException.class, () ->
                securityValidator.validatePopupHeaders("http://localhost:5500", "invalid-value")
        );
        assertTrue(ex2.getMessage().contains("CSRF protection"));
    }

    // 25. Security Audit: Origin không được phép (từ web lạ) bị từ chối
    @Test
    public void testGoogleSecurityValidator_UnauthorizedOrigin_ThrowsException() {
        GoogleAuthException ex = assertThrows(GoogleAuthException.class, () ->
                securityValidator.validatePopupHeaders("http://malicious-attacker.com", "XmlHttpRequest")
        );
        assertTrue(ex.getMessage().contains("Unauthorized request Origin"));
    }

    // 26. Security Audit: Google Token Endpoint trả response quá lớn (>1MB) bị từ chối
    @Test
    public void testExchangeCode_OversizedResponse_ThrowsException() {
        // Generate an oversized dummy JSON > 1MB
        String hugeString = "A".repeat(1024 * 1024 + 100);
        String hugeResponse = String.format("{\"id_token\": \"dummy\", \"padding\": \"%s\"}", hugeString);

        mockServer.expect(requestTo(TOKEN_URI))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(hugeResponse, MediaType.APPLICATION_JSON));

        GoogleServiceUnavailableException ex = assertThrows(GoogleServiceUnavailableException.class, () ->
                tokenVerifierService.exchangeCodeForIdToken("valid_code_huge_resp")
        );
        assertTrue(ex.getMessage().contains("maximum allowed size"));
    }

    // 27. Security Audit: Lỗi 400 không làm lộ thông tin nhạy cảm trong exception message
    @Test
    public void testExchangeCode_DoesNotLeakRawBodyInException() {
        String sensitiveErrorBody = "{\"error\": \"invalid_grant\", \"internal_secret_token\": \"SUPER_SENSITIVE_123\"}";

        mockServer.expect(requestTo(TOKEN_URI))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withBadRequest().body(sensitiveErrorBody).contentType(MediaType.APPLICATION_JSON));

        InvalidAuthorizationCodeException ex = assertThrows(InvalidAuthorizationCodeException.class, () ->
                tokenVerifierService.exchangeCodeForIdToken("any_code")
        );
        assertFalse(ex.getMessage().contains("SUPER_SENSITIVE_123"), "Exception message must NOT contain raw response body");
        assertEquals("Google authorization code is invalid or has expired", ex.getMessage());
    }

    // 28. Kiểm thử hồi quy: Đăng ký Candidate hỗ trợ đa dạng tên miền (Gmail, Outlook, School edu.vn)
    @Test
    public void testCandidateRegister_EmailMultiDomain_Valid() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();

        String[] validEmails = {
                "alex.student@gmail.com",
                "candidate@outlook.com",
                "student2026@hcmut.edu.vn",
                "nguyenvana@fpt.edu.vn",
                "test.user@company.com"
        };

        for (String email : validEmails) {
            CandidateRegisterRequest req = new CandidateRegisterRequest();
            req.setEmail(email);
            req.setPassword("Password123@");
            req.setFullName("Nguyen Van A");

            Set<ConstraintViolation<CandidateRegisterRequest>> violations = validator.validate(req);
            assertTrue(violations.isEmpty(), "Email hợp lệ phải pass validation: " + email);
        }
    }

    // 29. Kiểm thử hồi quy: Đăng ký Candidate từ chối email sai định dạng
    @Test
    public void testCandidateRegister_InvalidEmail_Rejected() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();

        String[] invalidEmails = {
                "plainaddress",
                "@missingusername.com",
                "missingdomain@.com",
                "two@@at.com",
                "spaces in@email.com"
        };

        for (String email : invalidEmails) {
            CandidateRegisterRequest req = new CandidateRegisterRequest();
            req.setEmail(email);
            req.setPassword("Password123@");
            req.setFullName("Nguyen Van A");

            Set<ConstraintViolation<CandidateRegisterRequest>> violations = validator.validate(req);
            assertFalse(violations.isEmpty(), "Email sai định dạng phải bị từ chối: " + email);
            assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("email")));
        }
    }

    // 30. Kiểm thử hồi quy: Đăng ký Employer hỗ trợ đa dạng tên miền (Gmail, Outlook, Doanh nghiệp)
    @Test
    public void testEmployerRegister_EmailMultiDomain_Valid() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();

        String[] validEmails = {
                "hr.recruiter@gmail.com",
                "recruiter@outlook.com",
                "talent@techcorp.vn",
                "hr@enterprise.com.vn",
                "careers@startup.io"
        };

        for (String email : validEmails) {
            EmployerRegisterRequest req = new EmployerRegisterRequest();
            req.setEmail(email);
            req.setPassword("Password123@");
            req.setFullName("Tran Thi HR");
            req.setCompanyId(1);

            Set<ConstraintViolation<EmployerRegisterRequest>> violations = validator.validate(req);
            assertTrue(violations.isEmpty(), "Email doanh nghiệp hợp lệ phải pass validation: " + email);
        }
    }

    // 31. Kiểm thử hồi quy: Đăng ký Employer từ chối email sai định dạng
    @Test
    public void testEmployerRegister_InvalidEmail_Rejected() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();

        String[] invalidEmails = {
                "invalid-emp-email",
                "@no-user.org",
                "spaces in@domain.com",
                "two@@at.com",
                "trailing-dot@domain."
        };

        for (String email : invalidEmails) {
            EmployerRegisterRequest req = new EmployerRegisterRequest();
            req.setEmail(email);
            req.setPassword("Password123@");
            req.setFullName("Tran Thi HR");
            req.setCompanyId(1);

            Set<ConstraintViolation<EmployerRegisterRequest>> violations = validator.validate(req);
            assertFalse(violations.isEmpty(), "Email doanh nghiệp sai định dạng phải bị từ chối: " + email);
        }
    }

    // 32. Kiểm thử hồi quy: Stale Key trong vòng 24 giờ được sử dụng an toàn khi Google JWKS tạm thời gián đoạn
    @Test
    public void testStaleKeyWithin24Hours_UsesStaleKeyWhenGoogleDown() throws Exception {
        jwksKeyProvider.registerKey("kid-recent", keyPair1.getPublic());

        // Set lastFetchTime to 2 hours ago (expired cache > 1h, but within 24h stale tolerance)
        java.lang.reflect.Field field = GoogleJwksKeyProvider.class.getDeclaredField("lastFetchTime");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        java.util.concurrent.atomic.AtomicReference<java.time.Instant> ref =
                (java.util.concurrent.atomic.AtomicReference<java.time.Instant>) field.get(jwksKeyProvider);
        ref.set(java.time.Instant.now().minus(java.time.Duration.ofHours(2)));

        // Google endpoint fails with 500
        mockServer.expect(requestTo(JWKS_URI))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withServerError());

        Date exp = new Date(System.currentTimeMillis() + 3600000);
        String token = buildJwt(keyPair1, "kid-recent", "https://accounts.google.com", CLIENT_ID,
                exp, "google-sub-recent", "user@gmail.com", true, "Recent User", null);

        // Verification must succeed with stale key and full signature verification
        GoogleTokenPayload payload = tokenVerifierService.verifyIdToken(token);
        assertNotNull(payload);
        assertEquals("google-sub-recent", payload.getSub());
        assertEquals("user@gmail.com", payload.getEmail());
    }

    // 33. Kiểm thử hồi quy: JWKS refresh đồng thời an toàn đa luồng (Thread-safe & No DoS)
    @Test
    public void testJwksRefresh_ConcurrentAccess_ThreadSafe() throws Exception {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    // Concurrent calls to getPublicKey
                    var key = jwksKeyProvider.getPublicKey("kid-1");
                    if (key != null) {
                        successCount.incrementAndGet();
                    }
                } catch (Exception ignored) {
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown(); // Release all threads at the exact same instant
        assertTrue(finishLatch.await(5, TimeUnit.SECONDS));
        executor.shutdown();

        assertEquals(threadCount, successCount.get(), "Mọi luồng đồng thời đều phải lấy được public key an toàn");
    }

    // 34. Kiểm thử hồi quy: Frontend gửi đúng header GIS Popup CSRF được chấp nhận
    @Test
    public void testSecurityValidator_FrontendCsrfHeaders_Accepted() {
        // Standard Frontend local origins
        assertDoesNotThrow(() ->
                securityValidator.validatePopupHeaders("http://localhost:5500", "XmlHttpRequest")
        );
        assertDoesNotThrow(() ->
                securityValidator.validatePopupHeaders("http://127.0.0.1:5500", "XmlHttpRequest")
        );
        // Case-insensitivity on XMLHttpRequest
        assertDoesNotThrow(() ->
                securityValidator.validatePopupHeaders("http://localhost:5500", "XMLHttpRequest")
        );
    }

    // 35. Kiểm thử hồi quy: Backend từ chối nhiều dạng Origin không nằm trong whitelist
    @Test
    public void testSecurityValidator_MultipleUnauthorizedOrigins_Rejected() {
        String[] unauthorizedOrigins = {
                "http://evil-hacker.com",
                "http://phishing-unipath.com",
                "https://accounts.google.com",
                "http://localhost:8080", // Backend port, not in allowed frontend origins
                "null"
        };

        for (String origin : unauthorizedOrigins) {
            GoogleAuthException ex = assertThrows(GoogleAuthException.class, () ->
                    securityValidator.validatePopupHeaders(origin, "XmlHttpRequest")
            );
            assertTrue(ex.getMessage().contains("Unauthorized request Origin") || ex.getMessage().contains("Missing Origin header"));
        }
    }
}

package com.example.unipathapi.auth.service;

import com.example.unipathapi.auth.dto.GoogleTokenPayload;
import com.example.unipathapi.auth.exception.GoogleServiceUnavailableException;
import com.example.unipathapi.auth.exception.InvalidAuthorizationCodeException;
import com.example.unipathapi.auth.exception.InvalidGoogleTokenException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.time.Duration;
import java.util.Base64;
import java.util.Date;

/**
 * Service responsible for Google ID Token verification and Google OAuth authorization code exchange.
 *
 * Rules:
 * - Strict RS256 signature verification using Google's JWKS public keys.
 * - Validates issuer (https://accounts.google.com or accounts.google.com).
 * - Validates audience against configured google.client-id.
 * - Validates token expiration, email_verified, and presence of Google sub and email.
 * - Exchanges authorization code with https://oauth2.googleapis.com/token using redirect_uri=postmessage.
 * - Never logs authorization codes, client secrets, access tokens, or raw tokens.
 * - Never returns client secret or access token to client.
 */
@Service
public class GoogleTokenVerifierService {

    private static final Logger log = LoggerFactory.getLogger(GoogleTokenVerifierService.class);

    private static final String GOOGLE_ISSUER_HTTPS = "https://accounts.google.com";
    private static final String GOOGLE_ISSUER_BARE = "accounts.google.com";
    private static final int MAX_RESPONSE_BYTES = 1024 * 1024; // 1 MB limit
    private static final java.util.regex.Pattern EMAIL_PATTERN =
            java.util.regex.Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final String expectedClientId;
    private final String clientSecret;
    private final String tokenUri;
    private final String redirectUri;

    private final GoogleJwksKeyProvider jwksKeyProvider;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    public GoogleTokenVerifierService(
            @Value("${google.client-id}") String expectedClientId,
            @Value("${google.client-secret:}") String clientSecret,
            @Value("${google.token-uri:https://oauth2.googleapis.com/token}") String tokenUri,
            @Value("${google.redirect-uri:postmessage}") String redirectUri,
            GoogleJwksKeyProvider jwksKeyProvider,
            @Autowired(required = false) ObjectMapper objectMapper) {
        this.expectedClientId = expectedClientId;
        this.clientSecret = clientSecret;
        this.tokenUri = tokenUri;
        this.redirectUri = redirectUri;
        this.jwksKeyProvider = jwksKeyProvider;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));
        this.restTemplate = new RestTemplate(requestFactory);
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    // Secondary constructor for unit testing with custom mocks
    public GoogleTokenVerifierService(
            String expectedClientId,
            String clientSecret,
            String tokenUri,
            String redirectUri,
            GoogleJwksKeyProvider jwksKeyProvider,
            RestTemplate restTemplate,
            ObjectMapper objectMapper) {
        this.expectedClientId = expectedClientId;
        this.clientSecret = clientSecret;
        this.tokenUri = tokenUri;
        this.redirectUri = redirectUri;
        this.jwksKeyProvider = jwksKeyProvider;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Verifies a Google ID Token string cryptographically against Google's public JWKS.
     *
     * @param idToken raw Google ID Token JWT
     * @return structured GoogleTokenPayload with verified claims
     */
    public GoogleTokenPayload verifyIdToken(String idToken) {
        if (idToken == null || idToken.isBlank()) {
            throw new InvalidGoogleTokenException("Google ID Token is empty or null");
        }

        String[] parts = idToken.trim().split("\\.");
        if (parts.length != 3) {
            throw new InvalidGoogleTokenException("Invalid JWT structure: token must consist of 3 parts");
        }

        // 1. Inspect Header without trusting payload
        String headerJson;
        try {
            headerJson = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new InvalidGoogleTokenException("Failed to Base64URL-decode JWT header", e);
        }

        String alg;
        String kid;
        try {
            JsonNode headerNode = objectMapper.readTree(headerJson);
            alg = headerNode.path("alg").asText(null);
            kid = headerNode.path("kid").asText(null);
        } catch (Exception e) {
            throw new InvalidGoogleTokenException("Failed to parse JWT header JSON", e);
        }

        if (!"RS256".equals(alg)) {
            throw new InvalidGoogleTokenException("Unsupported JWT algorithm: expected RS256, received: " + alg);
        }

        if (kid == null || kid.isBlank()) {
            throw new InvalidGoogleTokenException("JWT header missing key ID (kid)");
        }

        // 2. Resolve RSA Public Key from JWKS Provider (with key rotation support)
        PublicKey publicKey = jwksKeyProvider.getPublicKey(kid);
        if (publicKey == null) {
            throw new InvalidGoogleTokenException("No public key found in Google JWKS for kid: " + kid);
        }

        // 3. Cryptographically verify RS256 signature and extract Claims
        Claims claims;
        try {
            Jws<Claims> jws = Jwts.parserBuilder()
                    .setSigningKey(publicKey)
                    .build()
                    .parseClaimsJws(idToken.trim());
            claims = jws.getBody();
        } catch (ExpiredJwtException e) {
            throw new InvalidGoogleTokenException("Google ID Token has expired", e);
        } catch (JwtException | IllegalArgumentException e) {
            throw new InvalidGoogleTokenException("Google ID Token signature verification failed: " + e.getMessage(), e);
        }

        // 4. Validate Claims
        // Issuer check
        String issuer = claims.getIssuer();
        if (issuer == null || (!GOOGLE_ISSUER_HTTPS.equals(issuer) && !GOOGLE_ISSUER_BARE.equals(issuer))) {
            throw new InvalidGoogleTokenException("Invalid Google token issuer: " + issuer);
        }

        // Audience check: aud MUST contain the expected Google Client ID
        String audience = claims.getAudience();
        if (expectedClientId != null && !expectedClientId.isBlank()) {
            if (audience == null || !expectedClientId.equals(audience.trim())) {
                throw new InvalidGoogleTokenException("Google token audience mismatch: expected " + expectedClientId + ", received aud=" + audience);
            }

            // Authorized Party (azp) check: when present, MUST match expected Google Client ID
            String azp = claims.get("azp", String.class);
            if (azp != null && !expectedClientId.equals(azp.trim())) {
                throw new InvalidGoogleTokenException("Google token authorized party (azp) mismatch: expected " + expectedClientId + ", received azp=" + azp);
            }
        }

        // Expiration check
        Date expiration = claims.getExpiration();
        if (expiration == null || expiration.before(new Date())) {
            throw new InvalidGoogleTokenException("Google ID Token is expired");
        }

        // Subject check
        String sub = claims.getSubject();
        if (sub == null || sub.trim().isEmpty()) {
            throw new InvalidGoogleTokenException("Google ID Token missing subject (sub)");
        }
        if (sub.length() > 255) {
            throw new InvalidGoogleTokenException("Google ID Token subject exceeds maximum length of 255 characters");
        }

        // Email check
        String email = claims.get("email", String.class);
        if (email == null || email.trim().isEmpty()) {
            throw new InvalidGoogleTokenException("Google ID Token missing email");
        }
        if (email.length() > 150 || !EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new InvalidGoogleTokenException("Google ID Token invalid email format");
        }

        // Email verified check: must strictly be boolean true (not string, integer, or null)
        Object emailVerifiedObj = claims.get("email_verified");
        if (!(emailVerifiedObj instanceof Boolean b && b.booleanValue())) {
            throw new InvalidGoogleTokenException("Google email is not verified: " + email);
        }

        // Optional profile claims with bounded lengths
        String name = claims.get("name", String.class);
        if (name != null && name.length() > 150) {
            name = name.substring(0, 150);
        }
        String picture = claims.get("picture", String.class);
        if (picture != null && picture.length() > 500) {
            picture = picture.substring(0, 500);
        }

        return GoogleTokenPayload.builder()
                .sub(sub.trim())
                .email(email.trim().toLowerCase())
                .name(name != null ? name.trim() : null)
                .picture(picture != null ? picture.trim() : null)
                .emailVerified(true)
                .build();
    }

    /**
     * Exchanges a one-time Google authorization code for tokens via Google's token endpoint.
     *
     * @param authorizationCode Google authorization code from GIS Popup UX
     * @return raw id_token string from Google
     */
    public String exchangeCodeForIdToken(String authorizationCode) {
        if (authorizationCode == null || authorizationCode.isBlank()) {
            throw new InvalidAuthorizationCodeException("Google authorization code cannot be null or empty");
        }
        if (clientSecret == null || clientSecret.isBlank()) {
            log.error("Google OAuth client secret is not configured in application properties");
            throw new GoogleServiceUnavailableException("Google OAuth configuration is incomplete (client secret missing)");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("code", authorizationCode.trim());
        body.add("client_id", expectedClientId);
        body.add("client_secret", clientSecret);
        body.add("redirect_uri", redirectUri);
        body.add("grant_type", "authorization_code");

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);

        try {
            log.info("Exchanging authorization code with Google token endpoint: {}", tokenUri);
            ResponseEntity<String> response = restTemplate.postForEntity(tokenUri, requestEntity, String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new GoogleServiceUnavailableException("Google token endpoint responded with status: " + response.getStatusCode());
            }

            if (response.getBody().length() > MAX_RESPONSE_BYTES) {
                log.error("Google token endpoint response exceeded max allowed size of {} bytes", MAX_RESPONSE_BYTES);
                throw new GoogleServiceUnavailableException("Google token endpoint response exceeds maximum allowed size");
            }

            JsonNode responseJson = objectMapper.readTree(response.getBody());
            String idToken = responseJson.path("id_token").asText(null);
            if (idToken == null || idToken.isBlank()) {
                throw new InvalidGoogleTokenException("Google token response did not contain an id_token");
            }

            return idToken;
        } catch (HttpClientErrorException ex) {
            log.warn("Google token exchange failed with HTTP status {}", ex.getStatusCode());
            throw new InvalidAuthorizationCodeException("Google authorization code is invalid or has expired", ex);
        } catch (HttpServerErrorException | ResourceAccessException ex) {
            log.error("Google token endpoint unreachable or returned server error: {}", ex.getMessage());
            throw new GoogleServiceUnavailableException("Google token service is temporarily unreachable", ex);
        } catch (InvalidGoogleTokenException | InvalidAuthorizationCodeException | GoogleServiceUnavailableException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected error during Google token exchange: {}", ex.getMessage());
            throw new GoogleServiceUnavailableException("Unexpected error during Google authorization code exchange", ex);
        }
    }

    /**
     * High-level method: Exchanges authorization code with Google and verifies the returned ID Token.
     *
     * @param authorizationCode Google authorization code
     * @return verified GoogleTokenPayload
     */
    public GoogleTokenPayload exchangeCodeAndVerify(String authorizationCode) {
        String idToken = exchangeCodeForIdToken(authorizationCode);
        return verifyIdToken(idToken);
    }

    public String getExpectedClientId() {
        return expectedClientId;
    }

    public String getTokenUri() {
        return tokenUri;
    }

    public String getRedirectUri() {
        return redirectUri;
    }
}

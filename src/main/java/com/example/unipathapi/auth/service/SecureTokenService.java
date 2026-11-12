package com.example.unipathapi.auth.service;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Service managing cryptographically secure internal tokens for UniPath onboarding
 * and account-linking flows.
 *
 * Rules:
 * - Uses SecureRandom with 32 bytes (256 bits) of entropy.
 * - Encodes using URL-safe Base64 without padding (43 characters).
 * - Computes SHA-256 hash (64 hex characters) for database storage.
 * - Never logs or stores raw tokens.
 */
@Service
public class SecureTokenService {

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Generates a 32-byte cryptographically secure random token, Base64URL-encoded without padding.
     *
     * @return 43-character URL-safe raw token
     */
    public String generateSecureToken() {
        byte[] entropy = new byte[32];
        secureRandom.nextBytes(entropy);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(entropy);
    }

    /**
     * Computes the SHA-256 hash of a raw token and returns it as a lowercase 64-character hex string.
     * Only this hash is persisted in the database.
     *
     * @param rawToken raw token string
     * @return 64-character lowercase hex SHA-256 digest
     */
    public String hashToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new IllegalArgumentException("Token cannot be null or blank");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(64);
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 digest algorithm is not available in JVM", e);
        }
    }
}


package com.example.unipathapi.common.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

/**
 * JWT Utility component for issuing HS256 tokens.
 * Externalizes secret key via 'jwt.secret' configuration with fail-fast validation.
 */
@Component
public class JwtUtil {

    public static final String DEFAULT_FALLBACK_SECRET = "UniPath_SecretKey_Chuyen_Xy_Ly_Bao_Mat_2026_@#$!";

    @Value("${jwt.secret:" + DEFAULT_FALLBACK_SECRET + "}")
    private String secretKey = DEFAULT_FALLBACK_SECRET;

    private static final long EXPIRE_DURATION = 24 * 60 * 60 * 1000;

    public JwtUtil() {
    }

    public JwtUtil(String secretKey) {
        this.secretKey = secretKey;
        validateSecretKey();
    }

    @PostConstruct
    public void validateSecretKey() {
        if (secretKey == null || secretKey.trim().isEmpty()) {
            throw new IllegalStateException("JWT Secret Key không được để trống (jwt.secret)");
        }
        if (secretKey.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT Secret Key phải có độ dài tối thiểu 256 bits (32 bytes) cho thuật toán HS256");
        }
    }

    private Key getSignKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(Integer userID, String role) {
        return Jwts.builder()
                .setSubject(String.valueOf(userID))
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRE_DURATION))
                .signWith(getSignKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String getSecretKey() {
        return secretKey;
    }
}

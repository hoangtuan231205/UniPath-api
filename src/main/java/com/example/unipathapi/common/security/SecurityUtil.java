package com.example.unipathapi.common.security;

import com.example.unipathapi.user.entity.User;
import com.example.unipathapi.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;

/**
 * Security Utility component for parsing and validating Bearer JWT tokens.
 * Externalizes secret key via 'jwt.secret' configuration.
 */
@Component
public class SecurityUtil {

    @Value("${jwt.secret:" + JwtUtil.DEFAULT_FALLBACK_SECRET + "}")
    private String secretKey = JwtUtil.DEFAULT_FALLBACK_SECRET;

    @Autowired
    private UserRepository userRepository;

    public SecurityUtil() {
    }

    public SecurityUtil(String secretKey, UserRepository userRepository) {
        this.secretKey = secretKey;
        this.userRepository = userRepository;
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

    public Integer getCurrentUserId(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Chưa xác thực hoặc thiếu Header Authorization");
        }
        String token = authHeader.substring(7);
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSignKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            Integer userId = Integer.parseInt(claims.getSubject());
            
            // Security Enforcement: Validate account is active
            validateActiveUser(userId);
            return userId;
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().startsWith("403:")) {
                throw e;
            }
            throw new RuntimeException("Token không hợp lệ hoặc đã hết hạn", e);
        } catch (Exception e) {
            throw new RuntimeException("Token không hợp lệ hoặc đã hết hạn");
        }
    }

    public Integer getOptionalCurrentUserId(HttpServletRequest request) {
        if (request == null) return null;
        try {
            return getCurrentUserId(request);
        } catch (Exception e) {
            return null;
        }
    }

    public String getCurrentUserRole(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Chưa xác thực hoặc thiếu Header Authorization");
        }
        String token = authHeader.substring(7);
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSignKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.get("role", String.class);
        } catch (Exception e) {
            throw new RuntimeException("Token không hợp lệ hoặc đã hết hạn");
        }
    }

    public String getOptionalCurrentUserRole(HttpServletRequest request) {
        if (request == null) return null;
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        String token = authHeader.substring(7);
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSignKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.get("role", String.class);
        } catch (Exception e) {
            return null;
        }
    }

    public void validateActiveUser(Integer userId) {
        if (userId == null) return;
        User user = userRepository.findById(userId).orElse(null);
        if (user != null && Boolean.FALSE.equals(user.getIsActive())) {
            throw new RuntimeException("403: Tài khoản của bạn đã bị khóa, không thể thực hiện thao tác này");
        }
    }
}

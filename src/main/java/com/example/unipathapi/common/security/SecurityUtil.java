package com.example.unipathapi.common.security;

import com.example.unipathapi.user.entity.User;
import com.example.unipathapi.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.security.Key;

@Component
public class SecurityUtil {

    private static final String SECRET_KEY = "UniPath_SecretKey_Chuyen_Xy_Ly_Bao_Mat_2026_@#$!";

    @Autowired
    private UserRepository userRepository;

    private Key getSignKey() {
        return Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
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
            throw e;
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

    public void validateActiveUser(Integer userId) {
        if (userId == null) return;
        User user = userRepository.findById(userId).orElse(null);
        if (user != null && Boolean.FALSE.equals(user.getIsActive())) {
            throw new RuntimeException("403: Tài khoản của bạn đã bị khóa, không thể thực hiện thao tác này");
        }
    }
}

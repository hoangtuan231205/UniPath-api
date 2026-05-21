package com.example.unipathapi.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {
    private static final String SECRET_KEY = "UniPath_SecretKey_Chuyen_Xy_Ly_Bao_Mat_2026_@#$!";
    private static final long EXPIRE_DURATION = 24 * 60 * 60 * 1000;
    private Key getSignKey(){
        return Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
    }

    public String generateToken(Integer userID, String role){
        return Jwts.builder()
                .setSubject(String.valueOf(userID))
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRE_DURATION))
                .signWith(getSignKey(), SignatureAlgorithm.HS256)
                .compact();
    }
}
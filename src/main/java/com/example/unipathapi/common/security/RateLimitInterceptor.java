package com.example.unipathapi.common.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDateTime;

/**
 * Spring MVC HandlerInterceptor enforcing IP-based Rate Limiting on sensitive authentication endpoints:
 * - POST /api/auth/google/link (5 req / 60s)
 * - POST /api/auth/login (10 req / 60s)
 *
 * Emits HTTP 429 Too Many Requests with Retry-After header upon violation.
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    @Value("${app.rate-limit.enabled:true}")
    private boolean enabled;

    @Value("${app.rate-limit.google-link.limit:5}")
    private int googleLinkLimit;

    @Value("${app.rate-limit.google-link.duration-seconds:60}")
    private int googleLinkDurationSeconds;

    @Value("${app.rate-limit.google-auth.limit:10}")
    private int googleAuthLimit;

    @Value("${app.rate-limit.google-auth.duration-seconds:60}")
    private int googleAuthDurationSeconds;

    @Value("${app.rate-limit.login.limit:10}")
    private int loginLimit;

    @Value("${app.rate-limit.login.duration-seconds:60}")
    private int loginDurationSeconds;

    private final RateLimiter rateLimiter;
    private final ClientIpResolver clientIpResolver;

    @Autowired
    public RateLimitInterceptor(RateLimiter rateLimiter, ClientIpResolver clientIpResolver) {
        this.rateLimiter = rateLimiter;
        this.clientIpResolver = clientIpResolver;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!enabled) {
            return true;
        }

        String uri = request.getRequestURI();
        String method = request.getMethod();

        int limit;
        int durationSeconds;
        String prefix;

        if ("POST".equalsIgnoreCase(method) && uri != null && uri.endsWith("/api/auth/google/link")) {
            limit = googleLinkLimit;
            durationSeconds = googleLinkDurationSeconds;
            prefix = "rate:google_link:";
        } else if ("POST".equalsIgnoreCase(method) && uri != null && (uri.endsWith("/api/auth/google") || uri.endsWith("/api/auth/google/"))) {
            limit = googleAuthLimit;
            durationSeconds = googleAuthDurationSeconds;
            prefix = "rate:google_auth:";
        } else if ("POST".equalsIgnoreCase(method) && uri != null && uri.contains("/api/auth/google/onboarding/")) {
            limit = googleAuthLimit;
            durationSeconds = googleAuthDurationSeconds;
            prefix = "rate:google_onboarding:";
        } else if ("POST".equalsIgnoreCase(method) && uri != null && uri.endsWith("/api/auth/login")) {
            limit = loginLimit;
            durationSeconds = loginDurationSeconds;
            prefix = "rate:login:";
        } else {
            return true;
        }

        String clientIp = clientIpResolver.resolveClientIp(request);
        String key = prefix + clientIp;
        long windowMs = durationSeconds * 1000L;

        boolean allowed = rateLimiter.tryAcquire(key, limit, windowMs);
        if (!allowed) {
            long retryAfter = rateLimiter.getRemainingTimeSeconds(key, windowMs);
            if (retryAfter <= 0) {
                retryAfter = durationSeconds;
            }

            response.setStatus(429); // 429 Too Many Requests
            response.setHeader("Retry-After", String.valueOf(retryAfter));
            response.setContentType("application/json;charset=UTF-8");

            String json = String.format(
                    "{\"timestamp\":\"%s\",\"status\":429,\"error\":\"Too Many Requests\",\"message\":\"Quá nhiều yêu cầu. Vui lòng thử lại sau %d giây.\"}",
                    LocalDateTime.now(), retryAfter
            );
            response.getWriter().write(json);
            return false;
        }

        return true;
    }

    public RateLimiter getRateLimiter() {
        return rateLimiter;
    }
}


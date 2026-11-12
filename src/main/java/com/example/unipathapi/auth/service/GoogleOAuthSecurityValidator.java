package com.example.unipathapi.auth.service;

import com.example.unipathapi.auth.exception.GoogleAuthException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Security validator enforcing CSRF protection and origin validation
 * for Google Identity Services (GIS) Authorization Code Model (Popup UX).
 *
 * Rules:
 * - GIS Popup UX requires requests from browser to include header 'X-Requested-With: XmlHttpRequest'.
 *   (Browsers prevent cross-origin forms/scripts from setting custom headers without CORS preflight).
 * - Origin header must match allowed frontend origins.
 * - Prevents Cross-Site Request Forgery (CSRF) from unauthorized external domains.
 */
@Component
public class GoogleOAuthSecurityValidator {

    private static final Logger log = LoggerFactory.getLogger(GoogleOAuthSecurityValidator.class);
    public static final String X_REQUESTED_WITH_HEADER = "X-Requested-With";
    public static final String EXPECTED_X_REQUESTED_WITH = "XmlHttpRequest";

    private final Set<String> allowedOrigins;

    public GoogleOAuthSecurityValidator(
            @Value("${google.allowed-origins:http://localhost:5500,http://127.0.0.1:5500,http://localhost:3000}") String allowedOriginsConfig) {
        if (allowedOriginsConfig != null && !allowedOriginsConfig.isBlank()) {
            this.allowedOrigins = Arrays.stream(allowedOriginsConfig.split(","))
                    .map(String::trim)
                    .map(this::normalizeOrigin)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toSet());
        } else {
            this.allowedOrigins = Set.of();
        }
    }

    // Constructor for testing
    public GoogleOAuthSecurityValidator(List<String> origins) {
        this.allowedOrigins = origins.stream()
                .map(this::normalizeOrigin)
                .collect(Collectors.toSet());
    }

    /**
     * Validates headers from GIS Popup authorization code request.
     *
     * @param originHeader Value of Origin header
     * @param xRequestedWithHeader Value of X-Requested-With header
     */
    public void validatePopupHeaders(String originHeader, String xRequestedWithHeader) {
        // 1. Verify X-Requested-With header (GIS Popup CSRF defense)
        if (xRequestedWithHeader == null || !EXPECTED_X_REQUESTED_WITH.equalsIgnoreCase(xRequestedWithHeader.trim())) {
            log.warn("Rejected Google OAuth request: missing or invalid X-Requested-With header");
            throw new GoogleAuthException("Missing or invalid X-Requested-With header; request rejected by CSRF protection");
        }

        // 2. Verify Origin header
        if (originHeader == null || originHeader.isBlank()) {
            log.warn("Rejected Google OAuth request: missing Origin header");
            throw new GoogleAuthException("Missing Origin header in Google OAuth request");
        }

        String normalizedOrigin = normalizeOrigin(originHeader);
        if (!allowedOrigins.isEmpty() && !allowedOrigins.contains(normalizedOrigin)) {
            log.warn("Rejected Google OAuth request: unauthorized Origin: {}", normalizedOrigin);
            throw new GoogleAuthException("Unauthorized request Origin: " + normalizedOrigin);
        }
    }

    private String normalizeOrigin(String origin) {
        if (origin == null || origin.isBlank()) {
            return "";
        }
        try {
            URI uri = URI.create(origin.trim());
            String scheme = uri.getScheme() != null ? uri.getScheme().toLowerCase() : "";
            String host = uri.getHost() != null ? uri.getHost().toLowerCase() : "";
            int port = uri.getPort();

            if (port == -1 || (port == 80 && "http".equals(scheme)) || (port == 443 && "https".equals(scheme))) {
                return scheme + "://" + host;
            }
            return scheme + "://" + host + ":" + port;
        } catch (Exception e) {
            return origin.trim().toLowerCase();
        }
    }

    public Set<String> getAllowedOrigins() {
        return allowedOrigins;
    }
}


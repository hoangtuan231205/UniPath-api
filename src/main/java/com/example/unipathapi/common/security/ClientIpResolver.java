package com.example.unipathapi.common.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Utility component to extract client IP safely without blindly trusting spoofable proxy headers.
 */
@Component
public class ClientIpResolver {

    @Value("${app.rate-limit.trusted-proxy-enabled:false}")
    private boolean trustedProxyEnabled;

    public String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }

        String remoteAddr = request.getRemoteAddr();

        // Only inspect X-Forwarded-For or X-Real-IP if trusted proxy is enabled or remoteAddr is loopback
        if (trustedProxyEnabled || isLoopbackAddress(remoteAddr)) {
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isBlank()) {
                String[] parts = xForwardedFor.split(",");
                if (parts.length > 0 && !parts[0].trim().isEmpty()) {
                    return parts[0].trim();
                }
            }

            String xRealIp = request.getHeader("X-Real-IP");
            if (xRealIp != null && !xRealIp.isBlank()) {
                return xRealIp.trim();
            }
        }

        return remoteAddr != null ? remoteAddr : "unknown";
    }

    private boolean isLoopbackAddress(String addr) {
        if (addr == null) return false;
        return "127.0.0.1".equals(addr) || "0:0:0:0:0:0:0:1".equals(addr) || "::1".equals(addr);
    }
}


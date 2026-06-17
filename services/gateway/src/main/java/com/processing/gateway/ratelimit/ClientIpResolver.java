package com.processing.gateway.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

/**
 * Resolves the client IP address for rate limiting.
 *
 * <p>Forwarded headers are used before {@link HttpServletRequest#getRemoteAddr()}
 * because the gateway can run behind a reverse proxy</p>
 */
@Component
public class ClientIpResolver {
    // if we have a reverse proxy, without it, there is not much sense at all, since we can put any header ourselves
    private static final String X_FORWARDED_FOR = "X-Forwarded-For";
    private static final String X_REAL_IP = "X-Real-IP";
    private static final String UNKNOWN_IP = "unknown";

    /**
     * Resolves the best available client IP address from request headers
     *
     * @param request incoming servlet request
     * @return resolved client IP address
     */
    public String resolve(HttpServletRequest request) {
        String forwardedFor = request.getHeader(X_FORWARDED_FOR);
        if (hasText(forwardedFor)) {
            return forwardedFor.split(",", 2)[0].trim();
        }

        String realIp = request.getHeader(X_REAL_IP);
        if (hasText(realIp)) {
            return realIp.trim();
        }

        // try to get a remote ip, (we don't have a real reverse proxy)
        String remoteAddr = request.getRemoteAddr();
        return hasText(remoteAddr) ? remoteAddr : UNKNOWN_IP;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}

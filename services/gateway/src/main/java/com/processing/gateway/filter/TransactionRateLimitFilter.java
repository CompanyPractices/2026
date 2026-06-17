package com.processing.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.processing.gateway.ratelimit.ClientIpResolver;
import com.processing.gateway.ratelimit.InMemoryRateLimiter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

/**
 * Applies the configured per-second in-memory limit to transaction requests.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@RequiredArgsConstructor
public class TransactionRateLimitFilter extends OncePerRequestFilter {

    private static final String TRANSACTIONS_PATH = "/api/transactions";
    private static final String RATE_LIMIT_KEY_PREFIX = "POST " + TRANSACTIONS_PATH + ":";

    private final InMemoryRateLimiter rateLimiter;
    private final ClientIpResolver clientIpResolver;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String clientIp = clientIpResolver.resolve(request);
        String rateLimitKey = RATE_LIMIT_KEY_PREFIX + clientIp;

        if (!isTransactionRequest(request)
                || rateLimiter.allowRequest(rateLimitKey)) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader(HttpHeaders.RETRY_AFTER, "1");

        objectMapper.writeValue(response.getWriter(), Map.of(
                "error", "RATE_LIMIT_EXCEEDED",
                "message", "Too many requests. Try again later.",
                "retryAfterMs", 1000
        ));
    }

    private boolean isTransactionRequest(HttpServletRequest request) {
        return "POST".equalsIgnoreCase(request.getMethod())
                && TRANSACTIONS_PATH.equals(request.getRequestURI());
    }
}

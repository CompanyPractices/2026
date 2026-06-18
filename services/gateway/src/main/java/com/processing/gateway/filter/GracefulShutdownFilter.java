package com.processing.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.processing.common.dto.ServiceUnavailableResponse;
import com.processing.gateway.properties.ShutdownProperties;
import com.processing.gateway.shutdown.GracefulShutdownState;
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

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class GracefulShutdownFilter extends OncePerRequestFilter {
    private static final String GATEWAY_SERVICE_NAME = "gateway";

    private final GracefulShutdownState shutdownState;
    private final ShutdownProperties shutdownProperties;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        if (!shutdownState.isShuttingDown()) {
            filterChain.doFilter(request, response);
            return;
        }

        writeServiceUnavailable(response);
    }

    private void writeServiceUnavailable(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(shutdownProperties.retryAfterSeconds()));

        objectMapper.writeValue(response.getWriter(), new ServiceUnavailableResponse(
                "SERVICE_UNAVAILABLE",
                "Gateway is shutting down, retry later",
                GATEWAY_SERVICE_NAME
        ));
    }
}

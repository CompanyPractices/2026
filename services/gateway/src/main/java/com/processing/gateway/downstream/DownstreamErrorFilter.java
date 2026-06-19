package com.processing.gateway.downstream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.processing.common.dto.ServiceUnavailableResponse;
import com.processing.gateway.metrics.GatewayMetrics;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * Converts downstream connectivity failures into gateway-level HTTP 503 responses.
 *
 * <p>Unexpected exceptions and errors from unknown routes are rethrown so normal
 * Spring error handling can process them.</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 4)
@RequiredArgsConstructor
public class DownstreamErrorFilter extends OncePerRequestFilter {
    private final ObjectMapper objectMapper;
    private final DownstreamServiceResolver serviceResolver;
    private final GatewayMetrics gatewayMetrics;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        try {
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            Optional<String> serviceName = serviceResolver.resolve(request.getRequestURI());

            if (serviceName.isPresent()
                    && DownstreamExceptionUtils.isDownstreamUnavailable(e)
                    && !response.isCommitted()) {
                gatewayMetrics.recordDownstreamUnavailable(serviceName.get());
                writeServiceUnavailable(response, serviceName.get());
                return;
            }

            DownstreamExceptionUtils.rethrow(e);
        }
    }

    private void writeServiceUnavailable(HttpServletResponse response, String serviceName) throws IOException {
        response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        objectMapper.writeValue(response.getWriter(), new ServiceUnavailableResponse(
                "SERVICE_UNAVAILABLE",
                capitalize(serviceName) + " service is temporarily unavailable",
                serviceName
        ));
    }

    private String capitalize(String serviceName) {
        if (serviceName == null || serviceName.isBlank()) {
            return serviceName;
        }

        return serviceName.substring(0, 1).toUpperCase() + serviceName.substring(1);
    }

}

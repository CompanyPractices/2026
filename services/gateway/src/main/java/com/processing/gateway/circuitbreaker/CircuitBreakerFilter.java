package com.processing.gateway.circuitbreaker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.processing.common.dto.ServiceUnavailableResponse;
import com.processing.gateway.downstream.DownstreamServiceResolver;
import com.processing.gateway.downstream.DownstreamExceptionUtils;
import com.processing.gateway.metrics.GatewayMetrics;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Optional;

/**
 * Servlet filter that protects downstream routes with a per-service circuit breaker.
 *
 * <p>The downstream service is resolved from gateway route metadata. When the
 * circuit is open, the filter responds with HTTP 503 without calling the route.</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 3)
public class CircuitBreakerFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;
    private final DownstreamServiceResolver serviceResolver;
    private final InMemoryCircuitBreaker circuitBreaker;
    private final GatewayMetrics gatewayMetrics;
    private final Duration openDuration;

    /**
     * Creates the filter with its collaborators and configured open duration.
     *
     * @param objectMapper mapper used to write JSON error bodies
     * @param serviceResolver resolves request paths to downstream services
     * @param circuitBreaker in-memory circuit breaker
     * @param openDuration duration advertised through {@code Retry-After}
     */
    public CircuitBreakerFilter(ObjectMapper objectMapper,
                                DownstreamServiceResolver serviceResolver,
                                InMemoryCircuitBreaker circuitBreaker,
                                GatewayMetrics gatewayMetrics,
                                @Value("${gateway.circuit-breaker.open-duration:10s}") Duration openDuration) {
        this.objectMapper = objectMapper;
        this.serviceResolver = serviceResolver;
        this.circuitBreaker = circuitBreaker;
        this.gatewayMetrics = gatewayMetrics;
        this.openDuration = openDuration;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        Optional<String> serviceName = serviceResolver.resolve(request.getRequestURI());

        if (serviceName.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        String downstreamService = serviceName.get();
        if (!circuitBreaker.allowRequest(downstreamService)) {
            gatewayMetrics.recordCircuitOpen(downstreamService);
            writeCircuitOpen(response, downstreamService);
            return;
        }

        try {
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            if (DownstreamExceptionUtils.isDownstreamUnavailable(e)) {
                circuitBreaker.recordFailure(downstreamService);
            } else {
                circuitBreaker.releaseRequest(downstreamService);
            }

            DownstreamExceptionUtils.rethrow(e);
        }

        int status = response.getStatus();
        if (status >= HttpStatus.INTERNAL_SERVER_ERROR.value()) {
            circuitBreaker.recordFailure(downstreamService);
            return;
        }
        if (status == HttpStatus.NOT_FOUND.value()
                || status == HttpStatus.METHOD_NOT_ALLOWED.value()) {
            circuitBreaker.releaseRequest(downstreamService);
            return;
        }


        circuitBreaker.recordSuccess(downstreamService);
    }

    private void writeCircuitOpen(HttpServletResponse response, String serviceName) throws IOException {
        response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(openDuration.toSeconds()));

        objectMapper.writeValue(response.getWriter(), new ServiceUnavailableResponse(
                "SERVICE_UNAVAILABLE",
                serviceName + " service is temporarily unavailable",
                serviceName
        ));
    }

}

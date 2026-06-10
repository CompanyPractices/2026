package com.processing.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.processing.common.dto.ServiceUnavailableResponse;
import com.processing.gateway.circuitbreaker.InMemoryCircuitBreaker;
import com.processing.gateway.service.DownstreamServiceResolver;
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
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.Optional;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 3)
public class CircuitBreakerFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;
    private final DownstreamServiceResolver serviceResolver;
    private final InMemoryCircuitBreaker circuitBreaker;
    private final Duration openDuration;

    public CircuitBreakerFilter(ObjectMapper objectMapper,
                                DownstreamServiceResolver serviceResolver,
                                InMemoryCircuitBreaker circuitBreaker,
                                @Value("${gateway.circuit-breaker.open-duration:10s}") Duration openDuration) {
        this.objectMapper = objectMapper;
        this.serviceResolver = serviceResolver;
        this.circuitBreaker = circuitBreaker;
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
            writeCircuitOpen(response, downstreamService);
            return;
        }

        try {
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            if (isDownstreamUnavailable(e)) {
                circuitBreaker.recordFailure(downstreamService);
            } else {
                circuitBreaker.releaseRequest(downstreamService);
            }

            rethrow(e);
        }

        if (isFailureResponse(response.getStatus())) {
            circuitBreaker.recordFailure(downstreamService);
            return;
        }


        circuitBreaker.recordSuccess(downstreamService);
    }

    private boolean isDownstreamUnavailable(Throwable throwable) {
        Throwable current = throwable;

        while (current != null) {
            if (current instanceof ResourceAccessException
                    || current instanceof ConnectException
                    || current instanceof SocketTimeoutException
                    || current instanceof HttpTimeoutException
                    || current instanceof HttpConnectTimeoutException) {
                return true;
            }

            current = current.getCause();
        }

        return false;
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

    private void rethrow(Exception exception) throws ServletException, IOException {
        if (exception instanceof ServletException servletException) {
            throw servletException;
        }
        if (exception instanceof IOException ioException) {
            throw ioException;
        }
        if (exception instanceof RuntimeException runtimeException) {
            throw runtimeException;
        }

        throw new ServletException(exception);
    }

    private boolean isFailureResponse(int status) {
        return status >= HttpStatus.INTERNAL_SERVER_ERROR.value()
                || status == HttpStatus.NOT_FOUND.value()
                || status == HttpStatus.METHOD_NOT_ALLOWED.value();
    }
}

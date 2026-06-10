package com.processing.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.processing.common.dto.ServiceUnavailableResponse;
import com.processing.gateway.service.DownstreamServiceResolver;
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
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpTimeoutException;
import java.util.Optional;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class DownstreamErrorFilter extends OncePerRequestFilter {
    private final ObjectMapper objectMapper;
    private final DownstreamServiceResolver serviceResolver;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        try {
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            Optional<String> serviceName = serviceResolver.resolve(request.getRequestURI());

            if (serviceName.isPresent() && isDownstreamUnavailable(e) && !response.isCommitted()) {
                writeServiceUnavailable(response, serviceName.get());
                return;
            }

            rethrow(e);
        }
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
}

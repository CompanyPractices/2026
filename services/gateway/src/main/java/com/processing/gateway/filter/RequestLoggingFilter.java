package com.processing.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.processing.gateway.logging.RequestLog;
import com.processing.gateway.wrapper.MutableHeadersRequestWrapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Adds a request identifier and writes a structured log entry for each request.
 */
@Component
@Slf4j
public class RequestLoggingFilter extends OncePerRequestFilter {
    private final ObjectMapper mapper;

    private static final String ID_HEADER_NAME = "X-Request-Id";

    /**
     * Creates a request logging filter.
     *
     * @param mapper object mapper used to serialize structured log entries
     */
    public RequestLoggingFilter(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        long startTime = System.currentTimeMillis();

        String incoming = request.getHeader(ID_HEADER_NAME);
        String requestId = incoming != null ? incoming : UUID.randomUUID().toString();

        var wrappedRequest = new MutableHeadersRequestWrapper(request);
        wrappedRequest.setHeader(ID_HEADER_NAME, requestId);

        response.setHeader(ID_HEADER_NAME, requestId);

        MDC.put("requestId", requestId);

        try {
            filterChain.doFilter(wrappedRequest, response);
        } finally {
            long responseTime = System.currentTimeMillis() - startTime;

            var requestLog = RequestLog.builder()
                    .requestId(requestId)
                    .method(request.getMethod())
                    .path(request.getRequestURI())
                    .responseCode(response.getStatus())
                    .responseTime(responseTime)
                    .build();

            try {
                String mappedLog = mapper.writeValueAsString(requestLog);
                log.info(mappedLog);
            } catch (JsonProcessingException e) {
                log.error("Exception occurred while mapping log message", e);
            } finally {
                MDC.remove("requestId");
            }
        }
    }
}

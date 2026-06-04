package com.processing.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.processing.gateway.logger.RequestLog;
import com.processing.gateway.wrapper.MutableHeadersRequestWrapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class RequestLoggingFilter extends OncePerRequestFilter {
    private final ObjectMapper mapper;

    private final static String ID_HEADER_NAME = "X-Request-Id";

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        long startTime = System.currentTimeMillis();
        UUID requestId = UUID.randomUUID();

        var wrappedRequest = new MutableHeadersRequestWrapper(request);
        wrappedRequest.addHeader(ID_HEADER_NAME, requestId.toString());

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
                log.error("Exception occurred while mapping log message: {}", String.valueOf(e));
            }
        }
    }
}

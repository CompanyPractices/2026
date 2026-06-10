package com.processing.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.processing.common.dto.ServiceUnavailableResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.cache.Cache;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
@Slf4j
public class ResponseCachingFilter extends OncePerRequestFilter {
    private static final String CARDS_MGMT_SERVICE_PREFIX = "/api/cards";
    private static final String CACHE_HEADER_NAME = "X-Cache";
    private static final String CACHE_HIT = "HIT";
    private static final String CACHE_MISS = "MISS";

    private final Cache cache;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        if (!request.getRequestURI().startsWith(CARDS_MGMT_SERVICE_PREFIX)
                || !request.getMethod().equalsIgnoreCase(HttpMethod.GET.name())) {
            filterChain.doFilter(request, response);
            return;
        }

        String requestKey = request.getRequestURI() + request.getQueryString();
        String cachedBody = cache.get(requestKey, String.class);

        if (cachedBody != null) {
            response.setStatus(HttpServletResponse.SC_OK);
            response.setHeader(CACHE_HEADER_NAME, CACHE_HIT);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setContentLength(cachedBody.getBytes(StandardCharsets.UTF_8).length);
            response.getWriter().write(cachedBody);

            return;
        }

        var wrappedResponse = new ContentCachingResponseWrapper(response);

        try {
            filterChain.doFilter(request, wrappedResponse);

            if (wrappedResponse.getStatus() == HttpServletResponse.SC_OK) {
                cachedBody = new String(wrappedResponse.getContentAsByteArray(), StandardCharsets.UTF_8);
                cache.put(requestKey, cachedBody);
                response.setHeader(CACHE_HEADER_NAME, CACHE_MISS);
            }

            wrappedResponse.copyBodyToResponse();
        } catch (Exception e) {
            log.error("Exception occurred while caching response", e);
            response.setStatus(503);
            objectMapper.writeValue(response.getWriter(), new ServiceUnavailableResponse(
                    "SERVICE_UNAVAILABLE",
                    "Card Management Service is temporarily unavailable",
                    "Card Management Service"
            ));
        }
    }
}

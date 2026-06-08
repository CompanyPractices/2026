package com.processing.gateway.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
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

    private final Cache cache;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        if (!request.getRequestURI().contains(CARDS_MGMT_SERVICE_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String requestKey = request.getRequestURI() + request.getQueryString();
        String cachedBody = cache.get(requestKey, String.class);

        if (cachedBody != null) {
            response.setStatus(HttpServletResponse.SC_OK);
            response.setHeader("X-Cached", "true");
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setContentLength(cachedBody.length());
            response.getWriter().write(cachedBody);

            return;
        }

        var wrappedResponse = new ContentCachingResponseWrapper(response);

        try {
            filterChain.doFilter(request, wrappedResponse);

            if (wrappedResponse.getStatus() == HttpServletResponse.SC_OK) {
                cachedBody = new String(wrappedResponse.getContentAsByteArray(), StandardCharsets.UTF_8);
                cache.put(requestKey, cachedBody);
                wrappedResponse.copyBodyToResponse();
            } else {
                response.setStatus(502);
            }
        } catch (Exception e) {
            log.error("Exception occurred while caching response", e);
            response.setStatus(502);
        }
    }
}

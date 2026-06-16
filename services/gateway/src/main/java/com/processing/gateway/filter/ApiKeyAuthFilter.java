package com.processing.gateway.filter;

import com.processing.gateway.models.ApiKeyRoles;
import com.processing.gateway.models.ApiKey;
import com.processing.gateway.properties.ApiKeysProperties;
import com.processing.gateway.storage.ApiKeyStorage;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.cache.Cache;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class ApiKeyAuthFilter extends OncePerRequestFilter {
    private static final String API_KEY_HEADER = "X-API-Key";
    private static final String CACHE_HEADER_NAME = "X-Cache";
    private static final String CACHE_HIT = "HIT";
    private static final String CACHE_MISS = "MISS";

    private final ApiKeyStorage apiKeyStorage;
    private final ApiKeysProperties apiKeysProperties;
    private final Cache cache;
    private final AntPathMatcher antPathMatcher;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        for (ApiKeysProperties.Rules exclusion : apiKeysProperties.getExclusions()) {
            if (antPathMatcher.match(exclusion.getUrl(), request.getRequestURI())
                    && (request.getMethod().equals(exclusion.getMethod()) || exclusion.getMethod() == null)) {
                filterChain.doFilter(request, response);
                return;
            }
        }

        String key = request.getHeader(API_KEY_HEADER);

        if (key == null) {
            log.info("No header");
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return;
        }

        String cacheKey = "api_key_" + key;
        ApiKey apiKey = cache.get(cacheKey, ApiKey.class);

        if (apiKey == null) {
            apiKey = apiKeyStorage.get(key);

            if (apiKey == null) {
                log.info("Key not found");
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                return;
            }

            cache.put(cacheKey, apiKey);
            response.setHeader(CACHE_HEADER_NAME, CACHE_MISS);
        } else {
            response.setHeader(CACHE_HEADER_NAME, CACHE_HIT);
        }

        if (isAllowed(request, apiKey.role())) {
            filterChain.doFilter(request, response);
        } else {
            log.info("Not allowed");
            response.setStatus(HttpStatus.FORBIDDEN.value());
        }
    }

    private boolean isAllowed(HttpServletRequest request, ApiKeyRoles role) {
        if (role == ApiKeyRoles.ADMIN) {
            return true;
        }

        for (ApiKeysProperties.Rules rule : apiKeysProperties.getRules()) {
            if (antPathMatcher.match(rule.getUrl(), request.getRequestURI())
                    && (request.getMethod().equals(rule.getMethod()) || rule.getMethod() == null)
                    && role.compareTo(rule.getRole()) >= 0) {
                return true;
            }
        }

        return false;
    }
}

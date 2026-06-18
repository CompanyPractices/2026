package com.processing.gateway.openapi;

import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.servers.Server;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.util.List;

/**
 * Rewrites proxied downstream OpenAPI documents for public gateway exposure.
 *
 * <p>The filter removes internal routes and maps known internal transaction
 * paths back to their public gateway route before Swagger UI displays them.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OpenApiFilter extends OncePerRequestFilter {
    private static final String DOCS_ROUTE_SUFFIX = "-docs";
    private static final String INTERNAL_ROUTE_PREFIX = "/internal";
    private static final String TRANSACTIONS_INTERNAL_ROUTE = "/api/internal/route";
    private static final String TRANSACTIONS_PUBLIC_ROUTE = "/api/transactions";

    private final OpenApiProperties openApiProperties;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        if (!request.getRequestURI().endsWith(DOCS_ROUTE_SUFFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        var cachedResponse = new ContentCachingResponseWrapper(response);

        try {
            filterChain.doFilter(request, cachedResponse);

            byte[] responseBody = cachedResponse.getContentAsByteArray();

            OpenAPI openApi = Json.mapper().readValue(responseBody, OpenAPI.class);
            openApi.setServers(List.of(new Server().url(openApiProperties.getUrl())));

            Paths paths = openApi.getPaths();
            Paths adjustedPaths = new Paths();

            paths.forEach((k, v) -> {
                if (!k.contains(INTERNAL_ROUTE_PREFIX)) {
                    adjustedPaths.addPathItem(k, v);
                } else if (k.equals(TRANSACTIONS_INTERNAL_ROUTE)) {
                    adjustedPaths.addPathItem(TRANSACTIONS_PUBLIC_ROUTE, v);
                }
            });

            openApi.setPaths(adjustedPaths);

            byte[] modifiedResponseBody = Json.mapper().writeValueAsBytes(openApi);

            response.setContentLength(modifiedResponseBody.length);
            response.getOutputStream().write(modifiedResponseBody);
            response.getOutputStream().flush();
        } catch (Exception e) {
            log.error("Exception thrown while preparing OpenApi doc", e);
            response.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
        }
    }
}

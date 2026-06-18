package com.processing.gateway.filter;

import com.processing.gateway.openapi.OpenApiFilter;
import com.processing.gateway.openapi.OpenApiProperties;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiFilterTest {

    private final OpenApiFilter filter = new OpenApiFilter(openApiProperties());

    @Test
    void shouldSkipNonDocsRoutes() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        CountingFilterChain filterChain = new CountingFilterChain();
        request.setRequestURI("/api/transactions");

        filter.doFilter(request, response, filterChain);

        assertThat(filterChain.callCount).isEqualTo(1);
    }

    @Test
    void shouldRewriteOpenApiForGatewayPublicRoutes() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setRequestURI("/switch-docs");

        FilterChain filterChain = (servletRequest, servletResponse) -> {
            OpenAPI openApi = new OpenAPI()
                    .paths(new Paths()
                            .addPathItem("/api/internal/route", new PathItem())
                            .addPathItem("/internal/health", new PathItem())
                            .addPathItem("/api/public", new PathItem()));

            servletResponse.getOutputStream().write(Json.mapper().writeValueAsBytes(openApi));
        };

        filter.doFilter(request, response, filterChain);

        OpenAPI adjustedOpenApi = Json.mapper().readValue(response.getContentAsByteArray(), OpenAPI.class);
        assertThat(adjustedOpenApi.getServers()).hasSize(1);
        assertThat(adjustedOpenApi.getServers().getFirst().getUrl()).isEqualTo("http://gateway:8080");
        assertThat(adjustedOpenApi.getPaths().keySet())
                .containsExactlyInAnyOrder("/api/transactions", "/api/public");
    }

    private OpenApiProperties openApiProperties() {
        OpenApiProperties properties = new OpenApiProperties();
        properties.setUrl("http://gateway:8080");
        return properties;
    }

    private static final class CountingFilterChain implements FilterChain {
        private int callCount;

        @Override
        public void doFilter(jakarta.servlet.ServletRequest request,
                             jakarta.servlet.ServletResponse response) {
            callCount++;
        }
    }
}

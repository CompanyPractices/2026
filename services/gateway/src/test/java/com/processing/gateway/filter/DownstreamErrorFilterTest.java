package com.processing.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.processing.gateway.properties.GatewayRouteProperties;
import com.processing.gateway.service.DownstreamServiceResolver;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.client.ResourceAccessException;

import java.net.ConnectException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DownstreamErrorFilterTest {

    private final DownstreamErrorFilter filter = new DownstreamErrorFilter(
            new ObjectMapper(),
            new DownstreamServiceResolver(routeProperties())
    );

    @Test
    void returnsServiceUnavailableForSwitchConnectionFailure() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/transactions");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response,
                (servletRequest, servletResponse) -> {
            throw new ResourceAccessException("Connection refused",
                    new ConnectException("Connection refused"));
        });

        assertThat(response.getStatus()).isEqualTo(503);
        assertThat(response.getContentAsString()).contains(
                "\"error\":\"SERVICE_UNAVAILABLE\"",
                "\"serviceName\":\"switch\"",
                "switch service is temporarily unavailable"
        );
    }

    @Test
    void returnsServiceUnavailableForCardsConnectionFailure() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/cards/4000001234560001");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response,
                (servletRequest, servletResponse) -> {
            throw new ResourceAccessException("Connection refused",
                    new ConnectException("Connection refused"));
        });

        assertThat(response.getStatus()).isEqualTo(503);
        assertThat(response.getContentAsString()).contains(
                "\"serviceName\":\"cardManagement\""
        );
    }

    @Test
    void rethrowsNonDownstreamExceptions() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/transactions");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThatThrownBy(() -> filter.doFilter(request, response,
                (servletRequest, servletResponse) -> {
            throw new ServletException("Unexpected failure");
        }))
                .isInstanceOf(ServletException.class)
                .hasMessage("Unexpected failure");
    }

    @Test
    void rethrowsDownstreamExceptionForUnknownPath() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/health");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThatThrownBy(() -> filter.doFilter(request, response,
                (servletRequest, servletResponse) -> {
            throw new ResourceAccessException("Connection refused",
                    new ConnectException("Connection refused"));
        }))
                .isInstanceOf(ResourceAccessException.class)
                .hasMessage("Connection refused");
    }

    private GatewayRouteProperties routeProperties() {
        GatewayRouteProperties properties = new GatewayRouteProperties();
        properties.setRoutes(List.of(
                route("switch", "Path=/api/transactions"),
                route("cardManagement", "Path=/api/cards/**"),
                route("logger", "Path=/api/transactions/search"),
                route("logger", "Path=/api/dashboard/**"),
                route("terminalSimulator", "Path=/api/simulator/terminal/**"),
                route("merchantSimulator", "Path=/api/simulator/merchant/**")
        ));
        return properties;
    }

    private GatewayRouteProperties.RouteDefinition route(String serviceName, String pathPredicate) {
        GatewayRouteProperties.RouteDefinition route = new GatewayRouteProperties.RouteDefinition();
        route.setMetadata(Map.of("serviceName", serviceName));
        route.setPredicates(List.of(pathPredicate));
        return route;
    }
}

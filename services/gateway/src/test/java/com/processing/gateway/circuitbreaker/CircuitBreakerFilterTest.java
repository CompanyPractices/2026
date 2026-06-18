package com.processing.gateway.circuitbreaker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.processing.gateway.metrics.GatewayMetrics;
import com.processing.gateway.properties.GatewayRouteProperties;
import com.processing.gateway.downstream.DownstreamServiceResolver;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.client.ResourceAccessException;

import java.net.ConnectException;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CircuitBreakerFilterTest {

    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final InMemoryCircuitBreaker circuitBreaker = InMemoryCircuitBreaker.forTesting(
            Duration.ofSeconds(10),
            2,
            Clock.systemUTC()
    );
    private final CircuitBreakerFilter filter = new CircuitBreakerFilter(
            new ObjectMapper(),
            new DownstreamServiceResolver(routeProperties()),
            circuitBreaker,
            new GatewayMetrics(meterRegistry),
            Duration.ofSeconds(10)
    );

    @Test
    void returnsServiceUnavailableWithoutCallingDownstreamWhenCircuitIsOpen() throws Exception {
        circuitBreaker.recordFailure("switch");
        circuitBreaker.recordFailure("switch");

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/transactions");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean downstreamCalled = new AtomicBoolean(false);

        filter.doFilter(request, response,
                (servletRequest, servletResponse) -> downstreamCalled.set(true));

        assertThat(downstreamCalled).isFalse();
        assertThat(response.getStatus()).isEqualTo(503);
        assertThat(response.getHeader("Retry-After")).isEqualTo("10");
        assertThat(response.getContentAsString()).contains(
                "\"error\":\"SERVICE_UNAVAILABLE\"",
                "\"serviceName\":\"switch\""
        );
        assertThat(meterRegistry.counter(
                "gateway.requests.rejected",
                "reason", "circuit_open",
                "service", "switch"
        ).count()).isEqualTo(1);
    }

    @Test
    void recordsFailureForDownstreamConnectionException() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/transactions");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThatThrownBy(() -> filter.doFilter(request, response,
                (servletRequest, servletResponse) -> {
                    throw new ResourceAccessException("Connection refused",
                            new ConnectException("Connection refused"));
                }))
                .isInstanceOf(ResourceAccessException.class);

        assertThat(circuitBreaker.allowRequest("switch")).isTrue();

        assertThatThrownBy(() -> filter.doFilter(request, response,
                (servletRequest, servletResponse) -> {
                    throw new ResourceAccessException("Connection refused",
                            new ConnectException("Connection refused"));
                }))
                .isInstanceOf(ResourceAccessException.class);

        assertThat(circuitBreaker.allowRequest("switch")).isFalse();
    }

    @Test
    void recordsFailureForDownstreamServerErrorResponse() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/cards/4000001234560001");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response,
                (servletRequest, servletResponse) -> ((MockHttpServletResponse) servletResponse).setStatus(500));
        assertThat(circuitBreaker.allowRequest("cardManagement")).isTrue();

        filter.doFilter(request, response,
                (servletRequest, servletResponse) -> ((MockHttpServletResponse) servletResponse).setStatus(500));
        assertThat(circuitBreaker.allowRequest("cardManagement")).isFalse();
    }

    @Test
    void skipsUnknownPaths() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/health");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean downstreamCalled = new AtomicBoolean(false);

        filter.doFilter(request, response,
                (servletRequest, servletResponse) -> downstreamCalled.set(true));

        assertThat(downstreamCalled).isTrue();
    }

    private GatewayRouteProperties routeProperties() {
        GatewayRouteProperties properties = new GatewayRouteProperties();
        properties.setRoutes(List.of(
                route("switch", "Path=/api/transactions"),
                route("cardManagement", "Path=/api/cards/**")
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

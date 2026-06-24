package com.processing.gateway.circuitbreaker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.processing.gateway.downstream.DownstreamServiceResolver;
import com.processing.gateway.metrics.GatewayMetrics;
import com.processing.gateway.properties.GatewayRouteProperties;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.client.ResourceAccessException;
import reactor.core.publisher.Mono;

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
    void returnsServiceUnavailableWithoutCallingDownstreamWhenCircuitIsOpen() {
        circuitBreaker.recordFailure("switch");
        circuitBreaker.recordFailure("switch");

        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/api/transactions"));
        AtomicBoolean downstreamCalled = new AtomicBoolean(false);

        filter.filter(exchange, webExchange -> {
            downstreamCalled.set(true);
            return Mono.empty();
        }).block();

        assertThat(downstreamCalled).isFalse();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(exchange.getResponse().getHeaders().getFirst("Retry-After")).isEqualTo("10");
        assertThat(exchange.getResponse().getBodyAsString().block()).contains(
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
        MockServerWebExchange firstExchange = MockServerWebExchange.from(MockServerHttpRequest.post("/api/transactions"));

        assertThatThrownBy(() -> filter.filter(firstExchange, webExchange -> downstreamConnectionFailure()).block())
                .isInstanceOf(ResourceAccessException.class);

        assertThat(circuitBreaker.allowRequest("switch")).isTrue();

        MockServerWebExchange secondExchange = MockServerWebExchange.from(MockServerHttpRequest.post("/api/transactions"));

        assertThatThrownBy(() -> filter.filter(secondExchange, webExchange -> downstreamConnectionFailure()).block())
                .isInstanceOf(ResourceAccessException.class);

        assertThat(circuitBreaker.allowRequest("switch")).isFalse();
    }

    @Test
    void recordsFailureForDownstreamServerErrorResponse() {
        MockServerWebExchange firstExchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/cards/4000001234560001"));
        filter.filter(firstExchange, webExchange -> {
            webExchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
            return Mono.empty();
        }).block();
        assertThat(circuitBreaker.allowRequest("cardManagement")).isTrue();

        MockServerWebExchange secondExchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/cards/4000001234560001"));
        filter.filter(secondExchange, webExchange -> {
            webExchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
            return Mono.empty();
        }).block();
        assertThat(circuitBreaker.allowRequest("cardManagement")).isFalse();
    }

    @Test
    void skipsUnknownPaths() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/health"));
        AtomicBoolean downstreamCalled = new AtomicBoolean(false);

        filter.filter(exchange, webExchange -> {
            downstreamCalled.set(true);
            return Mono.empty();
        }).block();

        assertThat(downstreamCalled).isTrue();
    }

    private Mono<Void> downstreamConnectionFailure() {
        return Mono.error(new ResourceAccessException("Connection refused",
                new ConnectException("Connection refused")));
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

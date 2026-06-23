package com.processing.gateway.downstream;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.processing.gateway.metrics.GatewayMetrics;
import com.processing.gateway.properties.GatewayRouteProperties;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.client.ResourceAccessException;
import reactor.core.publisher.Mono;

import java.net.ConnectException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DownstreamErrorFilterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

    private final DownstreamErrorFilter filter = new DownstreamErrorFilter(
            objectMapper,
            new DownstreamServiceResolver(routeProperties()),
            new GatewayMetrics(meterRegistry)
    );

    @Test
    void returnsServiceUnavailableForSwitchConnectionFailure() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/api/transactions"));

        filter.filter(exchange, webExchange -> downstreamConnectionFailure()).block();

        Assertions.assertNotNull(exchange.getResponse().getStatusCode());
        assertThat(exchange.getResponse().getStatusCode().value()).isEqualTo(503);
        assertThat(exchange.getResponse().getBodyAsString().block()).contains(
                "\"error\":\"SERVICE_UNAVAILABLE\"",
                "\"serviceName\":\"switch\"",
                "Switch service is temporarily unavailable"
        );
        assertThat(meterRegistry.counter(
                "gateway.downstream.unavailable",
                "service", "switch"
        ).count()).isEqualTo(1);
    }

    @Test
    void returnsServiceUnavailableForAuthorizationConnectionFailure() throws Exception {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/internal/authorize"));

        filter.filter(exchange, webExchange -> downstreamConnectionFailure()).block();

        Map<String, String> body = objectMapper.readValue(
                exchange.getResponse().getBodyAsString().block(),
                new TypeReference<>() {
                });

        Assertions.assertNotNull(exchange.getResponse().getStatusCode());
        assertThat(exchange.getResponse().getStatusCode().value()).isEqualTo(503);
        assertThat(body).containsEntry("error", "SERVICE_UNAVAILABLE")
                .containsEntry("message", "Authorization service is temporarily unavailable")
                .containsEntry("serviceName", "authorization");
    }

    @Test
    void returnsServiceUnavailableForCardsConnectionFailure() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/cards/4000001234560001"));

        filter.filter(exchange, webExchange -> downstreamConnectionFailure()).block();

        Assertions.assertNotNull(exchange.getResponse().getStatusCode());
        assertThat(exchange.getResponse().getStatusCode().value()).isEqualTo(503);
        assertThat(exchange.getResponse().getBodyAsString().block()).contains(
                "\"serviceName\":\"cardManagement\""
        );
    }

    @Test
    void rethrowsNonDownstreamExceptions() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/api/transactions"));

        assertThatThrownBy(() -> filter.filter(exchange,
                webExchange -> Mono.error(new IllegalStateException("Unexpected failure"))).block())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Unexpected failure");
    }

    @Test
    void rethrowsDownstreamExceptionForUnknownPath() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/health"));

        assertThatThrownBy(() -> filter.filter(exchange, webExchange -> downstreamConnectionFailure()).block())
                .isInstanceOf(ResourceAccessException.class)
                .hasMessage("Connection refused");
    }

    private Mono<Void> downstreamConnectionFailure() {
        return Mono.error(new ResourceAccessException("Connection refused",
                new ConnectException("Connection refused")));
    }

    private GatewayRouteProperties routeProperties() {
        GatewayRouteProperties properties = new GatewayRouteProperties();
        properties.setRoutes(List.of(
                route("switch", "Path=/api/transactions"),
                route("authorization", "Path=/api/internal/authorize"),
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

package com.processing.gateway.shutdown;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.processing.gateway.circuitbreaker.CircuitBreakerFilter;
import com.processing.gateway.downstream.DownstreamErrorFilter;
import com.processing.gateway.metrics.GatewayMetrics;
import com.processing.gateway.ratelimit.TransactionRateLimitFilter;
import com.processing.gateway.validation.TransactionValidationFilter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class GracefulShutdownFilterTest {

    private final GracefulShutdownState shutdownState = new GracefulShutdownState();
    private final ShutdownProperties shutdownProperties = shutdownProperties();
    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final GracefulShutdownFilter filter = new GracefulShutdownFilter(
            shutdownState,
            shutdownProperties,
            new ObjectMapper(),
            new GatewayMetrics(meterRegistry)
    );

    @Test
    void allowsRequestsBeforeShutdownStarts() {
        AtomicInteger callCount = new AtomicInteger();
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/cards"));

        filter.filter(exchange, webExchange -> {
            callCount.incrementAndGet();
            webExchange.getResponse().setStatusCode(HttpStatus.OK);
            return Mono.empty();
        }).block();

        assertThat(callCount).hasValue(1);
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void rejectsNewRequestsAfterShutdownStarts() {
        shutdownState.startShutdown();
        AtomicInteger callCount = new AtomicInteger();
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/api/transactions"));

        filter.filter(exchange, webExchange -> {
            callCount.incrementAndGet();
            return Mono.empty();
        }).block();

        assertThat(callCount).hasValue(0);
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(exchange.getResponse().getHeaders().getFirst("Retry-After")).isEqualTo("30");
        assertThat(Objects.requireNonNull(exchange.getResponse().getHeaders().getContentType()).toString()).isEqualTo("application/json");
        assertThat(exchange.getResponse().getBodyAsString().block()).contains(
                "\"error\":\"SERVICE_UNAVAILABLE\"",
                "\"serviceName\":\"gateway\""
        );
        assertThat(meterRegistry.counter(
                "gateway.requests.rejected",
                "reason", "shutting_down",
                "service", "gateway"
        ).count()).isEqualTo(1);
    }

    @Test
    void hasHighestPriorityAmongGatewayFilters() {
        assertThat(filter.getOrder()).isLessThan(orderOf(TransactionRateLimitFilter.class));
        assertThat(filter.getOrder()).isLessThan(orderOf(TransactionValidationFilter.class));
        assertThat(filter.getOrder()).isLessThan(orderOf(CircuitBreakerFilter.class));
        assertThat(filter.getOrder()).isLessThan(orderOf(DownstreamErrorFilter.class));
    }

    private ShutdownProperties shutdownProperties() {
        ShutdownProperties properties = new ShutdownProperties();
        properties.setDrainPeriod(Duration.ofSeconds(30));
        return properties;
    }

    private int orderOf(Class<?> filterClass) {
        if (filterClass == TransactionRateLimitFilter.class) {
            return org.springframework.core.Ordered.HIGHEST_PRECEDENCE + 1;
        }
        if (filterClass == TransactionValidationFilter.class) {
            return org.springframework.core.Ordered.HIGHEST_PRECEDENCE + 2;
        }
        if (filterClass == CircuitBreakerFilter.class) {
            return org.springframework.core.Ordered.HIGHEST_PRECEDENCE + 3;
        }
        if (filterClass == DownstreamErrorFilter.class) {
            return org.springframework.core.Ordered.HIGHEST_PRECEDENCE + 4;
        }
        throw new IllegalArgumentException("Unsupported filter class: " + filterClass);
    }
}

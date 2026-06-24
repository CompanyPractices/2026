package com.processing.gateway.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.processing.gateway.metrics.GatewayMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionRateLimitFilterTest {
    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final TransactionRateLimitFilter filter = new TransactionRateLimitFilter(
            InMemoryRateLimiter.forTesting(1, 0, Duration.ofMinutes(10), 100, () -> 0),
            new ClientIpResolver(),
            new ObjectMapper(),
            new GatewayMetrics(meterRegistry)
    );

    @Test
    void appliesLimitPerClientIp() {
        AtomicInteger callCount = new AtomicInteger();

        MockServerWebExchange firstExchange = transactionExchange("203.0.113.10");
        filter.filter(firstExchange, successfulChain(callCount)).block();

        MockServerWebExchange secondExchange = transactionExchange("203.0.113.10");
        filter.filter(secondExchange, successfulChain(callCount)).block();

        MockServerWebExchange thirdExchange = transactionExchange("203.0.113.11");
        filter.filter(thirdExchange, successfulChain(callCount)).block();

        assertThat(firstExchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(secondExchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(thirdExchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(callCount).hasValue(2);
        assertThat(meterRegistry.counter(
                "gateway.requests.rejected",
                "reason", "rate_limit",
                "service", "gateway"
        ).count()).isEqualTo(1);

        filter.filter(transactionExchange("203.0.113.13"), successfulChain(callCount)).block();

        assertThat(callCount).hasValue(3);
    }

    @Test
    void skipsNonTransactionRequests() {
        AtomicInteger callCount = new AtomicInteger();
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest
                .method(HttpMethod.GET, "/api/transactions/search")
                .remoteAddress(new java.net.InetSocketAddress("203.0.113.10", 8080)));

        filter.filter(exchange, successfulChain(callCount)).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(callCount).hasValue(1);
    }

    private MockServerWebExchange transactionExchange(String clientIp) {
        return MockServerWebExchange.from(MockServerHttpRequest
                .method(HttpMethod.POST, "/api/transactions")
                .header("X-Forwarded-For", clientIp)
                .remoteAddress(new java.net.InetSocketAddress("127.0.0.1", 8080)));
    }

    private org.springframework.cloud.gateway.filter.GatewayFilterChain successfulChain(AtomicInteger callCount) {
        return exchange -> {
            callCount.incrementAndGet();
            exchange.getResponse().setStatusCode(HttpStatus.OK);
            return Mono.empty();
        };
    }
}

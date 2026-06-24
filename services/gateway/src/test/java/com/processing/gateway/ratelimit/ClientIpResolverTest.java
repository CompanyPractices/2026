package com.processing.gateway.ratelimit;

import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import java.net.InetSocketAddress;

import static org.assertj.core.api.Assertions.assertThat;

class ClientIpResolverTest {

    private final ClientIpResolver resolver = new ClientIpResolver();

    @Test
    void resolvesFirstIpFromXForwardedFor() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/")
                .header("X-Forwarded-For", "203.0.113.10, 10.0.0.1, 10.0.0.2")
                .header("X-Real-IP", "198.51.100.10")
                .remoteAddress(new InetSocketAddress("127.0.0.1", 8080)));

        assertThat(resolver.resolve(exchange)).isEqualTo("203.0.113.10");
    }

    @Test
    void fallsBackToXRealIp() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/")
                .header("X-Real-IP", "198.51.100.10")
                .remoteAddress(new InetSocketAddress("127.0.0.1", 8080)));

        assertThat(resolver.resolve(exchange)).isEqualTo("198.51.100.10");
    }

    @Test
    void fallsBackToRemoteAddr() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/")
                .remoteAddress(new InetSocketAddress("127.0.0.1", 8080)));

        assertThat(resolver.resolve(exchange)).isEqualTo("127.0.0.1");
    }
}

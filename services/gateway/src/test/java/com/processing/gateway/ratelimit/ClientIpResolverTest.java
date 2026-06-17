package com.processing.gateway.ratelimit;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class ClientIpResolverTest {

    private final ClientIpResolver resolver = new ClientIpResolver();

    @Test
    void resolvesFirstIpFromXForwardedFor() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "203.0.113.10, 10.0.0.1, 10.0.0.2");
        request.addHeader("X-Real-IP", "198.51.100.10");
        request.setRemoteAddr("127.0.0.1");

        assertThat(resolver.resolve(request)).isEqualTo("203.0.113.10");
    }

    @Test
    void fallsBackToXRealIp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Real-IP", "198.51.100.10");
        request.setRemoteAddr("127.0.0.1");

        assertThat(resolver.resolve(request)).isEqualTo("198.51.100.10");
    }

    @Test
    void fallsBackToRemoteAddr() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");

        assertThat(resolver.resolve(request)).isEqualTo("127.0.0.1");
    }
}

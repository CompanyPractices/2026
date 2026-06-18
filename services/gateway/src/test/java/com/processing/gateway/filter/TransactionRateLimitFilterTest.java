package com.processing.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.processing.gateway.ratelimit.ClientIpResolver;
import com.processing.gateway.ratelimit.InMemoryRateLimiter;
import com.processing.gateway.ratelimit.TransactionRateLimitFilter;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionRateLimitFilterTest {
    private final TransactionRateLimitFilter filter = new TransactionRateLimitFilter(
            InMemoryRateLimiter.forTesting(1, 0, Duration.ofMinutes(10), 100, () -> 0),
            new ClientIpResolver(),
            new ObjectMapper()
    );

    @Test
    void appliesLimitPerClientIp() throws Exception {
        CountingFilterChain filterChain = new CountingFilterChain();

        MockHttpServletResponse firstResponse = new MockHttpServletResponse();
        filter.doFilter(transactionRequest("203.0.113.10"), firstResponse, filterChain);

        MockHttpServletResponse secondResponse = new MockHttpServletResponse();
        filter.doFilter(transactionRequest("203.0.113.10"), secondResponse, filterChain);

        MockHttpServletResponse thirdResponse = new MockHttpServletResponse();
        filter.doFilter(transactionRequest("203.0.113.11"), thirdResponse, filterChain);

        assertThat(firstResponse.getStatus()).isEqualTo(200);
        assertThat(secondResponse.getStatus()).isEqualTo(429);
        assertThat(thirdResponse.getStatus()).isEqualTo(200);
        assertThat(filterChain.callCount).isEqualTo(2);

        MockHttpServletResponse fourthResponse = new MockHttpServletResponse();
        filter.doFilter(transactionRequest("203.0.113.13"), fourthResponse, filterChain);

        assertThat(filterChain.callCount).isEqualTo(3);
    }

    @Test
    void skipsNonTransactionRequests() throws Exception {
        CountingFilterChain filterChain = new CountingFilterChain();
        MockHttpServletRequest request = new MockHttpServletRequest(HttpMethod.GET.name(), "/api/transactions/search");
        request.setRemoteAddr("203.0.113.10");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(filterChain.callCount).isEqualTo(1);
    }

    private MockHttpServletRequest transactionRequest(String clientIp) {
        MockHttpServletRequest request = new MockHttpServletRequest(HttpMethod.POST.name(), "/api/transactions");
        request.addHeader("X-Forwarded-For", clientIp);
        request.setRemoteAddr("127.0.0.1");
        return request;
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

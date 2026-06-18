package com.processing.gateway.shutdown;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.processing.gateway.circuitbreaker.CircuitBreakerFilter;
import com.processing.gateway.downstream.DownstreamErrorFilter;
import com.processing.gateway.ratelimit.TransactionRateLimitFilter;
import com.processing.gateway.validation.TransactionValidationFilter;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.Order;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class GracefulShutdownFilterTest {

    private final GracefulShutdownState shutdownState = new GracefulShutdownState();
    private final ShutdownProperties shutdownProperties = shutdownProperties();
    private final GracefulShutdownFilter filter = new GracefulShutdownFilter(
            shutdownState,
            shutdownProperties,
            new ObjectMapper()
    );

    @Test
    void allowsRequestsBeforeShutdownStarts() throws Exception {
        CountingFilterChain filterChain = new CountingFilterChain();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(new MockHttpServletRequest("GET", "/api/cards"), response, filterChain);

        assertThat(filterChain.callCount).isEqualTo(1);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void rejectsNewRequestsAfterShutdownStarts() throws Exception {
        shutdownState.startShutdown();
        CountingFilterChain filterChain = new CountingFilterChain();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(new MockHttpServletRequest("POST", "/api/transactions"), response, filterChain);

        assertThat(filterChain.callCount).isZero();
        assertThat(response.getStatus()).isEqualTo(503);
        assertThat(response.getHeader("Retry-After")).isEqualTo("30");
        assertThat(response.getContentType()).isEqualTo("application/json");
        assertThat(response.getContentAsString()).contains(
                "\"error\":\"SERVICE_UNAVAILABLE\"",
                "\"serviceName\":\"gateway\""
        );
    }

    @Test
    void hasHighestPriorityAmongGatewayFilters() {
        int gracefulShutdownOrder = orderOf(GracefulShutdownFilter.class);

        assertThat(gracefulShutdownOrder).isLessThan(orderOf(TransactionRateLimitFilter.class));
        assertThat(gracefulShutdownOrder).isLessThan(orderOf(TransactionValidationFilter.class));
        assertThat(gracefulShutdownOrder).isLessThan(orderOf(CircuitBreakerFilter.class));
        assertThat(gracefulShutdownOrder).isLessThan(orderOf(DownstreamErrorFilter.class));
    }

    private ShutdownProperties shutdownProperties() {
        ShutdownProperties properties = new ShutdownProperties();
        properties.setDrainPeriod(Duration.ofSeconds(30));
        return properties;
    }

    private int orderOf(Class<?> filterClass) {
        return filterClass.getAnnotation(Order.class).value();
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

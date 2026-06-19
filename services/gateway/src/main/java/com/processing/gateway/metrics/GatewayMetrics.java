package com.processing.gateway.metrics;

import com.processing.gateway.common.models.Headers;
import com.processing.gateway.common.models.Services;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Publishes gateway-specific Micrometer counters used by the Grafana dashboard.
 */
@Component
@RequiredArgsConstructor
public class GatewayMetrics {

    private static final String REJECTED_REQUESTS = "gateway.requests.rejected";
    private static final String DOWNSTREAM_UNAVAILABLE = "gateway.downstream.unavailable";
    private static final String CACHE_REQUESTS = "gateway.cache.requests";
    private static final String CACHE_INVALIDATIONS = "gateway.cache.invalidations";
    private static final String CARDS_CACHE = "cards";

    private final MeterRegistry meterRegistry;

    /**
     * Registers expected series at startup so local dashboards show zeros before
     * the first gateway event happens.
     */
    @PostConstruct
    void registerMeters() {
        rejectedRequests("rate_limit", Services.GATEWAY.getValue());
        rejectedRequests("validation_invalid_json", Services.GATEWAY.getValue());
        rejectedRequests("validation_invalid_request", Services.GATEWAY.getValue());
        rejectedRequests("shutting_down", Services.GATEWAY.getValue());

        registerDownstreamUnavailable(Services.SWITCH.getValue());
        registerDownstreamUnavailable(Services.AUTH.getValue());
        registerDownstreamUnavailable(Services.CARDS.getValue());
        registerDownstreamUnavailable(Services.LOGGER.getValue());
        registerDownstreamUnavailable(Services.TERMINAL.getValue());
        registerDownstreamUnavailable(Services.MERCHANT.getValue());

        registerCardsCacheRequest(Headers.Values.CACHE_HIT.getValue());
        registerCardsCacheRequest(Headers.Values.CACHE_MISS.getValue());
        registerCardsCacheInvalidation();
    }

    public void recordRateLimitRejected() {
        rejectedRequests("rate_limit", Services.GATEWAY.getValue()).increment();
    }

    public void recordValidationRejected(String reason) {
        rejectedRequests("validation_" + reason, Services.GATEWAY.getValue()).increment();
    }

    public void recordCircuitOpen(String serviceName) {
        rejectedRequests("circuit_open", serviceName).increment();
    }

    public void recordGracefulShutdownRejected() {
        rejectedRequests("shutting_down", Services.GATEWAY.getValue()).increment();
    }

    public void recordDownstreamUnavailable(String serviceName) {
        registerDownstreamUnavailable(serviceName).increment();
    }

    public void recordCardsCacheHit() {
        recordCardsCacheRequest(Headers.Values.CACHE_HIT.getValue());
    }

    public void recordCardsCacheMiss() {
        recordCardsCacheRequest(Headers.Values.CACHE_MISS.getValue());
    }

    public void recordCardsCacheInvalidation() {
        registerCardsCacheInvalidation().increment();
    }

    private Counter rejectedRequests(String reason, String serviceName) {
        return Counter.builder(REJECTED_REQUESTS)
                .tag("reason", reason)
                .tag("service", serviceName)
                .register(meterRegistry);
    }

    private void recordCardsCacheRequest(String result) {
        registerCardsCacheRequest(result).increment();
    }

    private Counter registerDownstreamUnavailable(String serviceName) {
        return Counter.builder(DOWNSTREAM_UNAVAILABLE)
                .tag("service", serviceName)
                .register(meterRegistry);
    }

    private Counter registerCardsCacheRequest(String result) {
        return Counter.builder(CACHE_REQUESTS)
                .tag("cache", CARDS_CACHE)
                .tag("result", result)
                .register(meterRegistry);
    }

    private Counter registerCardsCacheInvalidation() {
        return Counter.builder(CACHE_INVALIDATIONS)
                .tag("cache", CARDS_CACHE)
                .register(meterRegistry);
    }
}

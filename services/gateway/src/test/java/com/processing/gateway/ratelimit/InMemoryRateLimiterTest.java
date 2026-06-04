package com.processing.gateway.ratelimit;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryRateLimiterTest {

    @Test
    void rejectsRequestsAboveLimitInSameSecond() {
        InMemoryRateLimiter limiter = new InMemoryRateLimiter(fixedClock("2026-02-01T10:30:00Z"));

        assertThat(limiter.allowRequest("POST /api/transactions", 2)).isTrue();
        assertThat(limiter.allowRequest("POST /api/transactions", 2)).isTrue();
        assertThat(limiter.allowRequest("POST /api/transactions", 2)).isFalse();
    }

    @Test
    void tracksDifferentKeysIndependently() {
        InMemoryRateLimiter limiter = new InMemoryRateLimiter(fixedClock("2026-01-01T10:30:00Z"));

        assertThat(limiter.allowRequest("POST /api/transactions", 1)).isTrue();
        assertThat(limiter.allowRequest("POST /api/transactions", 1)).isFalse();
        assertThat(limiter.allowRequest("GET /api/transactions/search", 1)).isTrue();
    }

    private Clock fixedClock(String instant) {
        return Clock.fixed(Instant.parse(instant), ZoneOffset.UTC);
    }
}

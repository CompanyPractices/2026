package com.processing.gateway.circuitbreaker;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryCircuitBreakerTest {

    @Test
    void opensCircuitAfterFailureThreshold() {
        MutableClock clock = new MutableClock("2026-06-01T10:30:00Z");
        InMemoryCircuitBreaker circuitBreaker = new InMemoryCircuitBreaker(Duration.ofSeconds(10), 2, clock);

        assertThat(circuitBreaker.allowRequest("switch")).isTrue();

        circuitBreaker.recordFailure("switch");
        assertThat(circuitBreaker.allowRequest("switch")).isTrue();

        circuitBreaker.recordFailure("switch");
        assertThat(circuitBreaker.allowRequest("switch")).isFalse();
    }

    @Test
    void allowsHalfOpenProbeAfterOpenDuration() {
        MutableClock clock = new MutableClock("2026-06-01T10:30:00Z");
        InMemoryCircuitBreaker circuitBreaker = new InMemoryCircuitBreaker(Duration.ofSeconds(10), 1, clock);

        circuitBreaker.recordFailure("switch");

        assertThat(circuitBreaker.allowRequest("switch")).isFalse();

        clock.advance(Duration.ofSeconds(10));

        assertThat(circuitBreaker.allowRequest("switch")).isTrue();
        assertThat(circuitBreaker.allowRequest("switch")).isFalse();
    }

    @Test
    void closesCircuitAfterSuccessfulHalfOpenProbe() {
        MutableClock clock = new MutableClock("2026-06-01T10:30:00Z");
        InMemoryCircuitBreaker circuitBreaker = new InMemoryCircuitBreaker(Duration.ofSeconds(10), 1, clock);

        circuitBreaker.recordFailure("switch");
        clock.advance(Duration.ofSeconds(10));

        assertThat(circuitBreaker.allowRequest("switch")).isTrue();
        circuitBreaker.recordSuccess("switch");

        assertThat(circuitBreaker.allowRequest("switch")).isTrue();
    }

    @Test
    void reopensCircuitAfterFailedHalfOpenProbe() {
        MutableClock clock = new MutableClock("2026-06-01T10:30:00Z");
        InMemoryCircuitBreaker circuitBreaker = new InMemoryCircuitBreaker(Duration.ofSeconds(10), 1, clock);

        circuitBreaker.recordFailure("switch");
        clock.advance(Duration.ofSeconds(10));

        assertThat(circuitBreaker.allowRequest("switch")).isTrue();
        circuitBreaker.recordFailure("switch");

        assertThat(circuitBreaker.allowRequest("switch")).isFalse();
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(String instant) {
            this.instant = Instant.parse(instant);
        }

        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}

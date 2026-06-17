package com.processing.gateway.ratelimit;

import com.github.benmanes.caffeine.cache.Ticker;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryRateLimiterTest {

    @Test
    void rejectsRequestsWhenBucketIsEmpty() {
        MutableTicker ticker = new MutableTicker();
        InMemoryRateLimiter limiter = limiter(2, 0, ticker);

        assertThat(limiter.allowRequest("POST /api/transactions:10.0.0.1")).isTrue();
        assertThat(limiter.allowRequest("POST /api/transactions:10.0.0.1")).isTrue();
        assertThat(limiter.allowRequest("POST /api/transactions:10.0.0.1")).isFalse();
    }

    @Test
    void tracksDifferentKeysIndependently() {
        MutableTicker ticker = new MutableTicker();
        InMemoryRateLimiter limiter = limiter(1, 0, ticker);

        assertThat(limiter.allowRequest("POST /api/transactions:10.0.0.1")).isTrue();
        assertThat(limiter.allowRequest("POST /api/transactions:10.0.0.1")).isFalse();
        assertThat(limiter.allowRequest("POST /api/transactions:10.0.0.2")).isTrue();
    }

    @Test
    void refillsTokensOverTime() {
        MutableTicker ticker = new MutableTicker();
        InMemoryRateLimiter limiter = limiter(2, 1, ticker);

        assertThat(limiter.allowRequest("POST /api/transactions:10.0.0.1")).isTrue();
        assertThat(limiter.allowRequest("POST /api/transactions:10.0.0.1")).isTrue();
        assertThat(limiter.allowRequest("POST /api/transactions:10.0.0.1")).isFalse();

        ticker.advance(Duration.ofSeconds(1));

        assertThat(limiter.allowRequest("POST /api/transactions:10.0.0.1")).isTrue();
        assertThat(limiter.allowRequest("POST /api/transactions:10.0.0.1")).isFalse();
    }

    @Test
    void expiresBucketsAfterAccessTimeout() {
        MutableTicker ticker = new MutableTicker();
        InMemoryRateLimiter limiter = new InMemoryRateLimiter(
                1,
                0,
                Duration.ofMinutes(10),
                10,
                ticker
        );

        assertThat(limiter.allowRequest("POST /api/transactions:10.0.0.1")).isTrue();
        assertThat(limiter.allowRequest("POST /api/transactions:10.0.0.1")).isFalse();

        ticker.advance(Duration.ofMinutes(11));
        limiter.cleanUp();

        assertThat(limiter.allowRequest("POST /api/transactions:10.0.0.1")).isTrue();
    }

    private InMemoryRateLimiter limiter(int capacity, double refillTokensPerSecond, Ticker ticker) {
        return new InMemoryRateLimiter(
                capacity,
                refillTokensPerSecond,
                Duration.ofMinutes(10),
                10,
                ticker
        );
    }

    private static final class MutableTicker implements Ticker {
        private long nanos;

        @Override
        public long read() {
            return nanos;
        }

        private void advance(Duration duration) {
            nanos += duration.toNanos();
        }
    }
}

package com.processing.gateway.ratelimit;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * In-memory fixed-window rate limiter used by gateway filters
 */
@Component
public class InMemoryRateLimiter {

    private final Clock clock;
    private final ConcurrentMap<String, RateLimitWindow> windows = new ConcurrentHashMap<>();

    /**
     * Creates a rate limiter that uses the system UTC clock
     */
    public InMemoryRateLimiter() {
        this(Clock.systemUTC());
    }

    InMemoryRateLimiter(Clock clock) {
        this.clock = clock;
    }

    /**
     * Checks whether a request is allowed in the current one-second window
     *
     * @param key bucket key used to separate independent limits
     * @param requestsPerSecond maximum number of requests allowed per second
     * @return {@code true} if the request is within the configured limit
     */
    public boolean allowRequest(String key, int requestsPerSecond) {
        RateLimitWindow window = windows.computeIfAbsent(key, ignored -> new RateLimitWindow());
        long currentSecond = clock.instant().getEpochSecond();

        synchronized (window) {
            if (window.epochSecond != currentSecond) {
                window.epochSecond = currentSecond;
                window.requestCount = 0;
            }
            window.requestCount++;
            return window.requestCount <= requestsPerSecond;
        }
    }

    private static final class RateLimitWindow {
        private long epochSecond = -1;
        private int requestCount;
    }
}

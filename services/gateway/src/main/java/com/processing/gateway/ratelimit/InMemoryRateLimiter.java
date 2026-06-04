package com.processing.gateway.ratelimit;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class InMemoryRateLimiter {

    private final Clock clock;
    private final ConcurrentMap<String, RateLimitWindow> windows = new ConcurrentHashMap<>();

    public InMemoryRateLimiter() {
        this(Clock.systemUTC());
    }

    InMemoryRateLimiter(Clock clock) {
        this.clock = clock;
    }

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

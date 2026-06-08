package com.processing.config;

import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public final class RetryFactory {

    private static final long MIN_INTERVAL_MS = 1;

    private static final Logger LOG = LoggerFactory.getLogger(RetryFactory.class);

    private RetryFactory() {
    }

    public static Retry loggerRetry(SwitchProperties properties) {
        return createRetry("logger", properties, true);
    }

    public static Retry authorizationRetry(SwitchProperties properties) {
        return createRetry("authorization", properties, false);
    }

    private static Retry createRetry(String name, SwitchProperties properties, boolean useBackoff) {
        SwitchProperties.RetryProperties retry = properties.retry();
        IntervalFunction intervalFunction = intervalFunction(retry, useBackoff);

        RetryConfig config = RetryConfig.custom()
                .maxAttempts(retry.maxAttempts())
                .intervalFunction(intervalFunction)
                .failAfterMaxAttempts(true)
                .build();

        Retry retryInstance = Retry.of(name, config);
        int maxAttempts = retry.maxAttempts();
        retryInstance.getEventPublisher()
                .onRetry(event -> LOG.warn(
                        "{} attempt {}/{} failed",
                        name,
                        event.getNumberOfRetryAttempts(),
                        maxAttempts,
                        event.getLastThrowable()))
                .onError(event -> LOG.error(
                        "{} unavailable after {} attempts",
                        name,
                        maxAttempts,
                        event.getLastThrowable()));

        return retryInstance;
    }

    private static IntervalFunction intervalFunction(SwitchProperties.RetryProperties retry, boolean useBackoff) {
        if (!useBackoff) {
            return IntervalFunction.of(MIN_INTERVAL_MS);
        }
        List<Long> backoffMs = retry.backoffMs();
        if (backoffMs == null || backoffMs.isEmpty()) {
            return IntervalFunction.of(MIN_INTERVAL_MS);
        }
        return attempt -> Math.max(
                MIN_INTERVAL_MS,
                backoffMs.get(Math.min(attempt, backoffMs.size() - 1)));
    }
}

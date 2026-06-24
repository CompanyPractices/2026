package com.processing.config;

import io.github.resilience4j.retry.Retry;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Unit-тесты {@link RetryFactory}: число попыток и exponential backoff для Logger. */
class RetryFactoryTest {

  /** Logger retry: 3 попытки с паузами 100 → 200 мс (production: 1s → 2s). */
  @Test
  void loggerRetry_appliesExponentialBackoffBetweenAttempts() {
    SwitchProperties properties = propertiesWithBackoff(List.of(100L, 200L, 400L));
    Retry retry = RetryFactory.loggerRetry(properties);
    List<Long> attemptTimestamps = new ArrayList<>();
    AtomicInteger calls = new AtomicInteger();

    assertThatThrownBy(() -> Retry.decorateCallable(retry, () -> {
      attemptTimestamps.add(System.currentTimeMillis());
      calls.incrementAndGet();
      throw new IllegalStateException("logger down");
    }).call()).isInstanceOf(Exception.class);

    assertThat(calls.get()).isEqualTo(3);
    assertThat(attemptTimestamps).hasSize(3);
    assertThat(gapMs(attemptTimestamps, 0)).isGreaterThanOrEqualTo(90L);
    assertThat(gapMs(attemptTimestamps, 1)).isGreaterThanOrEqualTo(190L);
  }

  private static long gapMs(List<Long> timestamps, int afterAttemptIndex) {
    return timestamps.get(afterAttemptIndex + 1) - timestamps.get(afterAttemptIndex);
  }

  private static SwitchProperties propertiesWithBackoff(List<Long> backoffMs) {
    return new SwitchProperties(
        "1.0.0",
        java.util.Map.of("400000", "ISS001"),
        "http://localhost:8083",
        "http://localhost:8088",
        "http://localhost:8086",
        new SwitchProperties.HttpProperties(3000, 5000, 2000),
        new SwitchProperties.RetryProperties(3, backoffMs),
        new SwitchProperties.CircuitBreakerSection(
            new SwitchProperties.CircuitBreakerProperties(50, 10, 5, 30_000L)));
  }
}

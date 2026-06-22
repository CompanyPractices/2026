package com.processing.cardmanagement.options;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.outbox")
public record OutboxOptions(int intervalMs, int maxRetryCount) {
}

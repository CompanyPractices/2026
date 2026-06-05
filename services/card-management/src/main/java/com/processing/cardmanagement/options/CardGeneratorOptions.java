package com.processing.cardmanagement.options;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.card-generator")
public record CardGeneratorOptions(
    int minBalance,
    int maxBalance,
    int minDailyLimit,
    int maxDailyLimit,
    String currencyCode
) {}

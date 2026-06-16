package com.processing.cardmanagement.options;

import com.processing.common.dto.annotations.DigitsOnly;
import com.processing.common.dto.annotations.NotNegative;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.card-service.defaults")
public record CardServiceDefaultsConfigurationProperties(

    @Positive
    int pageLimit,

    @NotNegative
    long pageOffset,

    @NotBlank
    @DigitsOnly
    String currencyCode,

    @NotNegative
    long dailyLimit,

    @NotNegative
    long monthlyLimit,

    @NotNegative
    long balance
) implements CardServiceDefaults {}

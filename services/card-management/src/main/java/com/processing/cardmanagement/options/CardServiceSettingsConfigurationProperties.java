package com.processing.cardmanagement.options;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.card-service")
public record CardServiceSettingsConfigurationProperties(

    @Positive
    int cardValidityPeriod,

    @Positive
    int maxPageLimit
) implements CardServiceSettings {}

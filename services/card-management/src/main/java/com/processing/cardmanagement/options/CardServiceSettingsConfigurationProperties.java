package com.processing.cardmanagement.options;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.card-service")
public record CardServiceSettingsConfigurationProperties(
    @NotBlank
    @Size(min = 1, max = 10)
    @Pattern(
        regexp = "^[A-Z0-9]+$"
    )
    String issuerId,

    @Positive
    int cardValidityPeriod,

    @Positive
    int maxPageLimit
) implements CardServiceSettings {}

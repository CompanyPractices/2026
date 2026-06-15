package com.processing.cardmanagement.options;

import com.processing.common.dto.annotations.DigitsOnly;
import com.processing.common.dto.annotations.NotNegative;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;

/**
 * Значения по умолчанию для CardService
 * Загружается из application.properties с префиксом app.card-service.defaults
 */
@Validated
@ConfigurationProperties(prefix = "app.card-service.defaults")
public record CardServiceDefaultsConfigurationProperties(

    @Positive
    long pageLimit,

    @NotNegative
    long pageOffset,

    @NotBlank
    @DigitsOnly
    String currencyCode,

    @NotNegative
    BigDecimal dailyLimit,

    @NotNegative
    BigDecimal monthlyLimit,

    @NotNegative
    BigDecimal balance
) {

    public CardServiceDefaults toCardServiceDefaults() {
        return new CardServiceDefaults(
            pageLimit,
            pageOffset,
            currencyCode,
            dailyLimit,
            monthlyLimit,
            balance
        );
    }
}

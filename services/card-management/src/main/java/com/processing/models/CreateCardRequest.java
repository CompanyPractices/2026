package com.processing.models;

import jakarta.validation.constraints.NotNull;

public record CreateCardRequest(
        @NotNull String bin,
        @NotNull String cardholderName,
        @NotNull String concurrencyCode,
        int dailyLimit,
        int monthlyLimit,
        int initialBalance
) {}

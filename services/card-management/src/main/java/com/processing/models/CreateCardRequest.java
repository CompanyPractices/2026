package com.processing.models;

import jakarta.validation.constraints.NotBlank;

public record CreateCardRequest(
    @NotBlank String bin,
    @NotBlank String cardholderName,
    @NotBlank String concurrencyCode,
    @NotBlank Integer dailyLimit,
    @NotBlank Integer monthlyLimit,
    @NotBlank Integer initialBalance
) {}

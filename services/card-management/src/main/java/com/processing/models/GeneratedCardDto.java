package com.processing.models;

public record GeneratedCardDto(
        String bin,
        String cardholderName,
        String currencyCode,
        int dailyLimit,
        int monthlyLimit,
        int balance,
        CardEntity.Status status
) {}

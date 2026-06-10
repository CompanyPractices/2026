package com.processing.cardmanagement.models;

public record CardDraft(
    String bin,
    String cardholderName,
    CardStatus status,
    String currencyCode,
    long dailyLimit,
    long monthlyLimit,
    long initialBalance
) {}

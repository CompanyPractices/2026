package com.processing.cardmanagement.options;

public record CardServiceDefaults(
    long pageLimit,
    long pageOffset,
    String currencyCode,
    long dailyLimit,
    long monthlyLimit,
    long balance
) {}

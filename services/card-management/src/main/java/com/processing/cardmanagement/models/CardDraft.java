package com.processing.cardmanagement.models;

import com.processing.common.dto.cardmanagement.CardStatus;

public record CardDraft(
    String bin,
    String cardholderName,
    CardStatus status,
    String currencyCode,
    long dailyLimit,
    long monthlyLimit,
    long initialBalance
) {}

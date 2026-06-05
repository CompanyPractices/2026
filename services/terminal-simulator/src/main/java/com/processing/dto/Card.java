package com.processing.dto;

import com.processing.model.CardStatus;

public record Card(
    String id,
    String pan,
    String bin,
    String cardholderName,
    String expiryDate,
    CardStatus status,
    String currencyCode,
    int dailyLimit,
    int monthlyLimit,
    int availableBalance,
    String issuerId,
    String createdAt
) {}

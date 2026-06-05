package com.processing.terminalsimulator.dto;

import com.processing.terminalsimulator.model.CardStatus;

public record Card(
    long id,
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

package com.processing.models;

import java.time.LocalDate;
import java.util.UUID;

public record CardDto(
        UUID id,
        String pan,
        String bin,
        String cardholderName,
        String expiryDate,
        String status,
        int dailyLimit,
        int monthlyLimit,
        int availableBalance,
        String issuerId,
        LocalDate createdAt
) {
    public static CardDto fromEntity(CardEntity entity) {
        return new CardDto(
                entity.getId(),
                entity.getPan(),
                entity.getBin(),
                entity.getCardholderName(),
                entity.getStrExpiryDate(),
                entity.getStatus().name(),
                entity.getDailyLimit(),
                entity.getMonthlyLimit(),
                entity.getAvailableBalance(),
                entity.getIssuerId(),
                entity.getCreatedAt()
        );
    }
}

package com.processing.models;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Card data transfer object")
public record CardDto(
    @Schema(description = "Unique identifier")
    UUID id,
    @Schema(description = "Card PAN", example = "4000001234567893")
    String pan,
    @Schema(description = "Bank Identification Number (BIN)", example = "400000")
    String bin,
    @Schema(description = "Cardholder name", example = "IVAN IVANOV")
    String cardholderName,
    @Schema(description = "Expiry date in MMYY format", example = "0629")
    String expiryDate,
    @Schema(description = "Card status", example = "ACTIVE", allowableValues = {"ACTIVE", "INACTIVE", "BLOCKED", "EXPIRED", "DELETED"})
    String status,
    @Schema(description = "Daily limit in kopecks", example = "15000000")
    long dailyLimit,
    @Schema(description = "Monthly limit in kopecks", example = "300000000")
    long monthlyLimit,
    @Schema(description = "Available balance in kopecks", example = "1000000000")
    long availableBalance,
    @Schema(description = "Issuer ID", example = "ZZZZZZ")
    String issuerId,
    @Schema(description = "Card creation date")
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

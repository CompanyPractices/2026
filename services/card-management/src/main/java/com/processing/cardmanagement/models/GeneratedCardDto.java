package com.processing.cardmanagement.models;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Generated card data")
public record GeneratedCardDto(
    @Schema(description = "Bank Identification Number (BIN)", example = "400000")
    String bin,
    @Schema(description = "Cardholder name", example = "IVAN IVANOV")
    String cardholderName,
    @Schema(description = "Currency code", example = "643")
    String currencyCode,
    @Schema(description = "Daily limit in kopecks", example = "15000000")
    int dailyLimit,
    @Schema(description = "Monthly limit in kopecks", example = "300000000")
    int monthlyLimit,
    @Schema(description = "Available balance in kopecks", example = "1000000000")
    int balance,
    @Schema(
        description = "Card status",
        example = "ACTIVE",
        allowableValues = {"ACTIVE",
        "INACTIVE",
        "BLOCKED",
        "EXPIRED",
        "DELETED"}
    )
    CardEntity.Status status
) {}

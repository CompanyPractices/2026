package com.processing.common.dto.cardmanagement;

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
    long dailyLimit,
    @Schema(description = "Monthly limit in kopecks", example = "300000000")
    long monthlyLimit,
    @Schema(description = "Available balance in kopecks", example = "1000000000")
    long balance,
    @Schema(description = "Card status", example = "ACTIVE", allowableValues = {"ACTIVE", "INACTIVE", "BLOCKED", "EXPIRED", "DELETED"})
    CardStatus status
) {}

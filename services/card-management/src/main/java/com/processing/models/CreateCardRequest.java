package com.processing.models;

import com.processing.annotations.Bin;
import com.processing.annotations.DigitsOnly;
import com.processing.annotations.ExactSize;
import com.processing.annotations.NotNegative;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Request to create a new card")
public record CreateCardRequest(

    @NotBlank
    @Bin
    @Schema(description = "Bank Identification Number (BIN)", example = "400000")
    String bin,

    @NotBlank
    @Pattern(
        regexp = "^[A-Z\\s\\-]+$",
        message = "Cardholder name can contain only uppercase letters, spaces and -"
    )
    @Schema(description = "Cardholder name", example = "IVAN IVANOV")
    String cardholderName,

    @NotBlank
    @ExactSize(3)
    @DigitsOnly
    @Schema(description = "Currency code", example = "643")
    String currencyCode,

    @NotNull
    @NotNegative
    @Schema(description = "Daily limit in kopecks", example = "15000000")
    Integer dailyLimit,

    @NotNull
    @NotNegative
    @Schema(description = "Monthly limit in kopecks", example = "300000000")
    Integer monthlyLimit,

    @NotNull
    @Schema(description = "Initial balance in kopecks", example = "1000000000")
    Integer initialBalance
) {}

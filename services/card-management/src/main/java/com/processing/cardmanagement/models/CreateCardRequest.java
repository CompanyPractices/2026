package com.processing.cardmanagement.models;

import com.processing.cardmanagement.annotations.Bin;
import com.processing.cardmanagement.annotations.DigitsOnly;
import com.processing.cardmanagement.annotations.ExactSize;
import com.processing.cardmanagement.annotations.NotNegative;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CreateCardRequest(

    @NotBlank
    @Bin
    String bin,

    @NotBlank
    @Pattern(
        regexp = "^[A-Z\\s\\-]+$",
        message = "Cardholder name can contain only uppercase letters, spaces and -"
    )
    String cardholderName,

    @NotBlank
    @ExactSize(3)
    @DigitsOnly
    String currencyCode,

    @NotNull
    @NotNegative
    Integer dailyLimit,

    @NotNull
    @NotNegative
    Integer monthlyLimit,

    @NotNull
    Integer initialBalance
) {}

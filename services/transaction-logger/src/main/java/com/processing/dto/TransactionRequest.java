package com.processing.dto;

import com.processing.enums.TransactionStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public record TransactionRequest(
        @NotNull
        UUID id,

        @NotBlank
        @Size(max = 4)
        String mti,

        @NotBlank
        @Size(max = 6)
        String stan,

        @Size(max = 12)
        String rrn,

        @NotBlank
        @Size(max = 16)
        String pan,

        @NotBlank
        @Size(max = 6)
        String processingCode,

        @NotNull
        @Positive
        Long amount,

        @NotBlank
        @Size(max = 3)
        String currencyCode,

        @NotBlank
        @Size(max = 8)
        String terminalId,

        @NotBlank
        @Size(max = 15)
        String merchantId,

        @NotBlank
        @Size(max = 4)
        String mcc,

        @NotBlank
        @Size(max = 10)
        String acquirerId,

        @NotBlank
        @Size(max = 10)
        String issuerId,

        Long acquiringFee,

        @NotNull
        TransactionStatus status,

        @Size(max = 100)
        String declineReason,

        @Size(max = 6)
        String authCode,

        @PositiveOrZero
        Integer processingTimeMs,

        @NotNull
        Instant transmissionDateTime,

        @NotNull
        Instant createdAt
) {
}

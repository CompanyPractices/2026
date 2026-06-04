package com.processing.dto;

import com.processing.enums.TransactionStatus;

import java.time.Instant;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        String mti,
        String stan,
        String rrn,
        String pan,
        String processingCode,
        Long amount,
        String currencyCode,
        String terminalId,
        String merchantId,
        String mcc,
        String acquirerId,
        String issuerId,
        Long acquiringFee,
        TransactionStatus status,
        String declineReason,
        String authCode,
        Integer processingTimeMs,
        Instant transmissionDateTime,
        Instant createdAt
) {
}

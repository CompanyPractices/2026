package com.processing.common.dto.transaction;


import java.time.Instant;
import java.util.UUID;


public record Transaction(
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
) {}

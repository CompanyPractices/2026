package com.processing.model;

import java.time.Instant;
import java.time.LocalDateTime;
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
        String terminalType,
        String merchantId,
        String mcc,
        String acquirerId,
        String issuerId,
        String status,
        String responseCode,
        String declineReason,
        String authCode,
        Integer processingTimeMs,
        LocalDateTime transmissionDateTime,
        Instant createdAt,
        Long acquiringFee
) {}

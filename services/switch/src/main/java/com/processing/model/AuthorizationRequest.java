package com.processing.model;

import java.time.LocalDateTime;

public record AuthorizationRequest(
        String mti,
        String stan,
        String pan,
        String processingCode,
        Long amount,
        String currencyCode,
        LocalDateTime transmissionDateTime,
        String terminalId,
        String terminalType,
        String merchantId,
        String mcc,
        String acquirerId,
        String issuerId
) {
    public AuthorizationRequest withIssuerId(String issuerId) {
        return new AuthorizationRequest(
                mti, stan, pan, processingCode, amount, currencyCode,
                transmissionDateTime, terminalId, terminalType, merchantId, mcc, acquirerId, issuerId);
    }
}

package com.processing.dto;

import java.math.BigDecimal;

public record AuthorizationRequest(
        String mti,
        String stan,
        String pan,
        String processingCode,
        BigDecimal amount,
        String currencyCode,
        String transmissionDateTime,
        String terminalId,
        String terminalType,
        String merchantId,
        String mcc,
        String acquirerId
) {
}

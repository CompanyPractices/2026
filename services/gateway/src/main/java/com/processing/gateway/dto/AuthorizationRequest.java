package com.processing.gateway.dto;

public record AuthorizationRequest(
        String mti,
        String stan,
        String pan,
        String processingCode,
        Integer amount,
        String currencyCode,
        String transmissionDateTime,
        String terminalId,
        TerminalType terminalType,
        String merchantId,
        String mcc,
        String acquirerId
) {
}

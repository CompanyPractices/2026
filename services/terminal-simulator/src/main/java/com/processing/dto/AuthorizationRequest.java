package com.processing.dto;

public record AuthorizationRequest(
    String mti,
    String stan,
    String pan,
    String processingCode,
    long amount,
    String currencyCode,
    String transmissionDateTime,
    String terminalId,
    String terminalType,
    String merchantId,
    String mcc,
    String acquirerId,
    String issuerId
) {}

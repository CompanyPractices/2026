package com.processing.merchantacquirer.dto;

public record TransactionResponse(
        String mti,
        String stan,
        String rrn,
        String authCode,
        String responseCode,
        String status,
        Integer processingTimeMs
) {
}

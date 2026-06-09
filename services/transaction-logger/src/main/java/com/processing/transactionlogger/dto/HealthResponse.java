package com.processing.transactionlogger.dto;

public record HealthResponse(
        String status,
        String service,
        Long transactionsStored
) {}

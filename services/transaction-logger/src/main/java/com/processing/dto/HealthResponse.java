package com.processing.dto;

public record HealthResponse(
        String status,
        String service,
        Long transactionsStored
) {}

package com.processing.common.dto;

public record ErrorResponse(
        String error,
        String message,
        String timestamp,
        String serviceName,
        String retryAfterMs
) {
}

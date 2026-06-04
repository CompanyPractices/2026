package com.processing.merchantacquirer.exception;

public record ErrorResponse(
        String error,
        String message,
        String timestamp,
        String serviceName,
        String retryAfterMs
) {
}

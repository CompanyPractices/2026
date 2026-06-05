package com.processing.terminalsimulator;

public record ErrorResponse(
        String error,
        String message,
        String timestamp,
        String serviceName,
        String retryAfterMs
) {
}

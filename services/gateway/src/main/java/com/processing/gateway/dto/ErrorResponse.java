package com.processing.gateway.dto;

public record ErrorResponse(
        String error,
        String message,
        String timestamp
) {
}

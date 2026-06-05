package com.processing.gateway.dto;

public record ServiceUnavailableResponse(
        String error,
        String message,
        String serviceName
) {
}

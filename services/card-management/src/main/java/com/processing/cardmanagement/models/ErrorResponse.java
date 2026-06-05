package com.processing.cardmanagement.models;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Error response")
public record ErrorResponse(
    @Schema(description = "Error class name")
    String error,
    @Schema(description = "Error message", example = "Card with present PAN was not found")
    String message,
    @Schema(description = "Timestamp of the error")
    LocalDateTime timestamp,
    @Schema(description = "Service name")
    String serviceName
) {

    public ErrorResponse(Exception exception) {
        this(
            exception.getClass().getName(),
            exception.getMessage(),
            LocalDateTime.now(),
            "card-management"
        );
    }
}

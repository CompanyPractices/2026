package com.processing.cardmanagement.models;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;

@Schema(description = "Error response")
public record ErrorResponse(
    @Schema(description = "HTTP status code", example = "404")
    int status,
    @Schema(description = "Error class name")
    String error,
    @Schema(description = "Error message", example = "Card with present PAN was not found")
    String message,
    @Schema(description = "Timestamp of the error")
    LocalDateTime timestamp
) {

    public ErrorResponse(
        HttpStatus status,
        Exception exception
    ) {
        this(
            status.value(),
            exception.getClass().getName(),
            exception.getMessage(),
            LocalDateTime.now()
        );
    }

    public ResponseEntity<ErrorResponse> toResponseEntity() {
        return ResponseEntity.status(status).body(this);
    }
}

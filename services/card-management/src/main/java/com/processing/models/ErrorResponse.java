package com.processing.models;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;

public record ErrorResponse(
    int status,
    String error,
    String message,
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

package com.processing.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleDataIntegrityViolationReturnsConflict() {
        ResponseEntity<Map<String, String>> response = handler.handleDataIntegrityViolation();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).containsEntry("error", "Data integrity violation");
    }

    @Test
    void handleDatabaseAccessReturnsServiceUnavailable() {
        ResponseEntity<Map<String, String>> response = handler.handleDatabaseAccess();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).containsEntry("error", "Database operation failed");
    }
}

package com.processing.exception;

import com.processing.common.dto.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
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
    void handleDataAccessReturnsServiceUnavailable() {
        ResponseEntity<ErrorResponse> response =
                handler.handleDataAccess(new DataAccessResourceFailureException("db down"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody().error()).isEqualTo("DB_UNAVAILABLE");
    }
}

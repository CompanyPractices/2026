package com.processing.transactionlogger.exception;

import com.processing.common.dto.ErrorResponse;
import com.processing.common.dto.transactionlogger.TransactionRequest;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.lang.reflect.Method;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleValidationReturnsBadRequest() throws NoSuchMethodException {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "transactionRequest");
        bindingResult.addError(new FieldError("transactionRequest", "id", "must not be null"));
        Method method = GlobalExceptionHandlerTest.class.getDeclaredMethod(
                "validationTarget",
                TransactionRequest.class
        );
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(
                new org.springframework.core.MethodParameter(method, 0),
                bindingResult
        );

        ResponseEntity<ErrorResponse> response = handler.handleValidation(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Validation error", response.getBody().error());
        assertThat(response.getBody().message()).contains("id");
        assertEquals("transaction-logger", response.getBody().serviceName());
    }

    @Test
    void handleMalformedJsonReturnsBadRequest() {
        ResponseEntity<ErrorResponse> response = handler.handleMalformedJson();

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Invalid request body", response.getBody().error());
        assertEquals("transaction-logger", response.getBody().serviceName());
    }

    @Test
    void handleTransactionConflictReturnsConflict() {
        UUID id = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

        ResponseEntity<ErrorResponse> response = handler.handleTransactionConflict(
                new TransactionConflictException(id)
        );

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Transaction conflict", response.getBody().error());
        assertThat(response.getBody().message()).contains(id.toString());
    }

    @Test
    void handleDataIntegrityViolationReturnsConflict() {
        ResponseEntity<ErrorResponse> response = handler.handleDataIntegrityViolation(
                new DataIntegrityViolationException("duplicate transaction")
        );

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Data integrity violation", response.getBody().error());
    }

    @Test
    void handleDatabaseAccessReturnsServiceUnavailable() {
        ResponseEntity<ErrorResponse> response = handler.handleDatabaseAccess(
                new DataAccessResourceFailureException("database unavailable")
        );

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Database operation failed", response.getBody().error());
        assertEquals("1000", response.getBody().retryAfterMs());
    }

    @Test
    void handleInternalErrorReturnsInternalServerError() {
        ResponseEntity<ErrorResponse> response = handler.handleInternalError(
                new RuntimeException("unexpected error")
        );

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Internal server error", response.getBody().error());
    }

    @SuppressWarnings("unused")
    private void validationTarget(TransactionRequest request) {
    }
}

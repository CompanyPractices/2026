package com.processing.exception;

import com.processing.common.dto.ErrorResponse;
import com.processing.dto.TransactionRequest;
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

class GlobalExceptionHandlerTest {

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

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error()).isEqualTo("Validation error");
        assertThat(response.getBody().message()).contains("id");
        assertThat(response.getBody().serviceName()).isEqualTo("transaction-logger");
    }

    @Test
    void handleMalformedJsonReturnsBadRequest() {
        ResponseEntity<ErrorResponse> response = handler.handleMalformedJson();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error()).isEqualTo("Invalid request body");
        assertThat(response.getBody().serviceName()).isEqualTo("transaction-logger");
    }

    @Test
    void handleTransactionConflictReturnsConflict() {
        UUID id = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

        ResponseEntity<ErrorResponse> response = handler.handleTransactionConflict(
                new TransactionConflictException(id)
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error()).isEqualTo("Transaction conflict");
        assertThat(response.getBody().message()).contains(id.toString());
    }

    @Test
    void handleDataIntegrityViolationReturnsConflict() {
        ResponseEntity<ErrorResponse> response = handler.handleDataIntegrityViolation(
                new DataIntegrityViolationException("duplicate transaction")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error()).isEqualTo("Data integrity violation");
    }

    @Test
    void handleDatabaseAccessReturnsServiceUnavailable() {
        ResponseEntity<ErrorResponse> response = handler.handleDatabaseAccess(
                new DataAccessResourceFailureException("database unavailable")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error()).isEqualTo("Database operation failed");
        assertThat(response.getBody().retryAfterMs()).isEqualTo("1000");
    }

    @Test
    void handleInternalErrorReturnsInternalServerError() {
        ResponseEntity<ErrorResponse> response = handler.handleInternalError(
                new RuntimeException("unexpected error")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error()).isEqualTo("Internal server error");
    }

    @SuppressWarnings("unused")
    private void validationTarget(TransactionRequest request) {
    }
}

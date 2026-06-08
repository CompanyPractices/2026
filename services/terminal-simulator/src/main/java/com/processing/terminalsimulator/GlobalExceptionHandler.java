package com.processing.terminalsimulator;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.time.LocalDateTime;


@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        ErrorResponse response = new ErrorResponse("Validation failed", ex.getMessage(),
                LocalDateTime.now().toString(), "terminal-simulator", String.valueOf(0));
        return ResponseEntity.badRequest().body(response);
    }
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex) {
        ErrorResponse response = new ErrorResponse("Illegal state", ex.getMessage(),
                LocalDateTime.now().toString(), "terminal-simulator", String.valueOf(0));
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
    }
    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<ErrorResponse> handleResourceAccess(ResourceAccessException ex) {
        ErrorResponse response = new ErrorResponse("Resource access", ex.getMessage(),
                LocalDateTime.now().toString(), "terminal-simulator", String.valueOf(0));
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }
    @ExceptionHandler(HttpClientErrorException.class)
    public ResponseEntity<ErrorResponse> handleHttpClientError(HttpClientErrorException ex) {
        ErrorResponse response = new ErrorResponse("Http connection error", ex.getMessage(),
                LocalDateTime.now().toString(), "terminal-simulator", String.valueOf(0));
        return ResponseEntity.status(ex.getStatusCode()).body(response);
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        ex.printStackTrace();
        ErrorResponse response = new ErrorResponse("Unexpected internal server error", ex.getMessage(),
                LocalDateTime.now().toString(), "terminal-simulator", String.valueOf(0));
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}

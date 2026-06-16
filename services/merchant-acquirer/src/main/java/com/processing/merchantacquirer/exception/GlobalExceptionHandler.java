package com.processing.merchantacquirer.exception;

import com.processing.common.dto.ErrorResponse;
import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;

@RestControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler(ExternalServiceException.class)
  public ResponseEntity<ErrorResponse> handleExceptionFromAnotherService(
      ExternalServiceException ex) {
    return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
        .body(
            new ErrorResponse(
                "External Service Error",
                ex.getMessage(),
                String.valueOf(LocalDateTime.now()),
                ex.getServiceName(),
                ex.getRetryAfterMs()));
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException ex) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(
            new ErrorResponse(
                "Invalid request",
                ex.getMessage(),
                String.valueOf(LocalDateTime.now()),
                "Merchant acquirer simulator",
                "5"));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleNotValid(MethodArgumentNotValidException ex) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(
            new ErrorResponse(
                "Invalid request",
                ex.getMessage(),
                String.valueOf(LocalDateTime.now()),
                "Merchant acquirer simulator",
                "5"));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleNotValid(IllegalArgumentException ex) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(
            new ErrorResponse(
                "Invalid request",
                ex.getMessage(),
                String.valueOf(LocalDateTime.now()),
                "Merchant acquirer simulator",
                "5"));
  }

  @ExceptionHandler(NullPointerException.class)
  public ResponseEntity<ErrorResponse> handleNullPointer(NullPointerException ex) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(
                    new ErrorResponse(
                            "Request processing failed",
                            ex.getMessage(),
                            String.valueOf(LocalDateTime.now()),
                            "Merchant acquirer simulator",
                            "5"));
  }

  @ExceptionHandler(HttpClientErrorException.class)
  public ResponseEntity<ErrorResponse> handleNotReadable(HttpClientErrorException ex) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(
                    new ErrorResponse(
                            "Invalid request",
                            ex.getMessage(),
                            String.valueOf(LocalDateTime.now()),
                            "Merchant acquirer simulator",
                            "5"));
  }
}

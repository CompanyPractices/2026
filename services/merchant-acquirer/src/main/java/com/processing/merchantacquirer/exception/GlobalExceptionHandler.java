package com.processing.merchantacquirer.exception;

import com.processing.common.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<ErrorResponse> handleExceptionFromAnotherService (ExternalServiceException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(new ErrorResponse(
                        "External Service Error",
                        ex.getMessage(),
                        String.valueOf(LocalDateTime.now()),
                        ex.getServiceName(),
                        ex.getRetryAfterMs()
                ));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException ex){
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(
                        "Invalid request",
                        ex.getMessage(),
                        String.valueOf(LocalDateTime.now()),
                        "Merchant acquirer simulator",
                        "5"
                ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleNotValid(MethodArgumentNotValidException ex){
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(
                        "Invalid request",
                        ex.getMessage(),
                        String.valueOf(LocalDateTime.now()),
                        "Merchant acquirer simulator",
                        "5"
                ));
    }
}

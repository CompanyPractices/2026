package com.processing.cardmanagement.configuration;

import com.processing.cardmanagement.exceptions.CardNotFoundException;
import com.processing.cardmanagement.exceptions.InsufficientFundsException;
import com.processing.cardmanagement.models.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CardNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCardNotFoundException(
        CardNotFoundException ex
    ) {
        return new ErrorResponse(HttpStatus.NOT_FOUND, ex).toResponseEntity();
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(
        ConstraintViolationException ex
    ) {
        return new ErrorResponse(HttpStatus.BAD_REQUEST, ex).toResponseEntity();
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
        MethodArgumentNotValidException ex
    ) {
        return new ErrorResponse(HttpStatus.BAD_REQUEST, ex).toResponseEntity();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
        IllegalArgumentException ex
    ) {
        return new ErrorResponse(HttpStatus.BAD_REQUEST, ex).toResponseEntity();
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(
        IllegalStateException ex
    ) {
        return new ErrorResponse(HttpStatus.BAD_REQUEST, ex).toResponseEntity();
    }

    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientFundsException(
        InsufficientFundsException ex
    ) {
        return new ErrorResponse(HttpStatus.PAYMENT_REQUIRED, ex).toResponseEntity();
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(
        Exception ex
    ) {
        return new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, ex).toResponseEntity();
    }
}

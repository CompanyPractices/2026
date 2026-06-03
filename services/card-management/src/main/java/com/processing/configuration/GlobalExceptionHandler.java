package com.processing.configuration;

import com.processing.exceptions.CardNotFoundException;
import com.processing.models.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;

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

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(
        Exception ex
    ) {
        return new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, ex).toResponseEntity();
    }
}

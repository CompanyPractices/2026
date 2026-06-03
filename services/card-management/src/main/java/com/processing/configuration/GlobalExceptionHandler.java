package com.processing.configuration;

import com.processing.exceptions.CardNotFoundException;
import com.processing.models.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(
        Exception ex
    ) {
        return new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, ex).toResponseEntity();
    }
}

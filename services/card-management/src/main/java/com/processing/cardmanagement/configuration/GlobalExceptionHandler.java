package com.processing.cardmanagement.configuration;

import com.processing.cardmanagement.exceptions.CardNotFoundException;
import com.processing.cardmanagement.exceptions.InsufficientFundsException;
import com.processing.common.dto.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CardNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleCardNotFoundException(
        CardNotFoundException ex
    ) {
        log.warn(ex.getMessage());
        return errorResponseFromException(ex);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleConstraintViolationException(
        ConstraintViolationException ex
    ) {
        log.warn(ex.getMessage());
        return errorResponseFromException(ex);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMethodArgumentNotValidException(
        MethodArgumentNotValidException ex
    ) {
        log.warn(ex.getMessage());
        return errorResponseFromException(ex);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleIllegalArgumentException(
        IllegalArgumentException ex
    ) {
        log.warn(ex.getMessage());
        return errorResponseFromException(ex);
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleIllegalStateException(
        IllegalStateException ex
    ) {
        log.warn(ex.getMessage());
        return errorResponseFromException(ex);
    }

    @ExceptionHandler(InsufficientFundsException.class)
    @ResponseStatus(HttpStatus.PAYMENT_REQUIRED)
    public ErrorResponse handleInsufficientFundsException(
        InsufficientFundsException ex
    ) {
        log.warn(ex.getMessage());
        return errorResponseFromException(ex);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleException(
        Exception ex
    ) {
        log.error(ex.getMessage());
        return errorResponseFromException(ex);
    }

    private ErrorResponse errorResponseFromException(Exception ex) {
        return new ErrorResponse(
            ex.getClass().getSimpleName(),
            ex.getMessage(),
            LocalDateTime.now().toString(),
            null,
            null
        );
    }
}

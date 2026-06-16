package com.processing.cardmanagement.configuration;

import com.processing.cardmanagement.exceptions.BinAlreadyExistException;
import com.processing.cardmanagement.exceptions.BinNotFoundException;
import com.processing.cardmanagement.exceptions.CardNotFoundException;
import com.processing.cardmanagement.exceptions.InsufficientFundsException;
import com.processing.common.dto.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @Value("${app.service-name}")
    private String serviceName;

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
        var violation = ex.getConstraintViolations()
                .stream()
                .findFirst();

        if (violation.isEmpty()) {
            log.warn(ex.getMessage());
        } else {
            log.warn(
                    "Message: {}, Invalid value: {}",
                    ex.getMessage(),
                    violation.get().getInvalidValue()
            );
        }

        return errorResponseFromException(ex);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex
    ) {
        var fieldError = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst();

        var errorMessage = fieldError
                .map(FieldError::getDefaultMessage)
                .orElse("Constraint violation");

        if (fieldError.isEmpty()) {
            log.warn(ex.getMessage());
        } else {
            log.warn(
                    "Message: {}, Field: {}, Invalid value: {}",
                    fieldError.get().getDefaultMessage(),
                    fieldError.get().getField(),
                    fieldError.get().getRejectedValue()
            );
        }

        return new ErrorResponse(
                ex.getClass().getSimpleName(),
                errorMessage,
                LocalDateTime.now().toString(),
                serviceName,
                null
        );
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
        log.error("Critical error: {}", ex.getMessage(), ex);
        return errorResponseFromException(ex);
    }

    @ExceptionHandler(BinNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleBinNotFoundException(
            BinNotFoundException ex
    ) {
        log.warn(ex.getMessage());
        return errorResponseFromException(ex);
    }

    @ExceptionHandler(BinAlreadyExistException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleBinAlreadyExistException(
            BinAlreadyExistException ex
    ) {
        log.warn(ex.getMessage());
        return errorResponseFromException(ex);
    }

    private ErrorResponse errorResponseFromException(Exception ex) {
        return new ErrorResponse(
                ex.getClass().getSimpleName(),
                ex.getMessage(),
                LocalDateTime.now().toString(),
                serviceName,
                null
        );
    }
}

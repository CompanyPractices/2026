package com.processing.cardmanagement.configuration;

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
import java.util.Objects;

/**
 * Глобальный обработчик исключений
 * Преобразует исключения в HTTP ответы с соответствующими статус-кодами
 */
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
        log.warn(ex.getMessage());
        return errorResponseFromException(ex);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMethodArgumentNotValidException(
        MethodArgumentNotValidException ex
    ) {
        String errorMessage = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(FieldError::getDefaultMessage)
            .filter(Objects::nonNull)
            .findFirst()
            .orElse("Validation failed");

        log.warn(errorMessage);
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

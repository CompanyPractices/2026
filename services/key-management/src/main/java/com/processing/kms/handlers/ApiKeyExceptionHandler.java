package com.processing.kms.handlers;

import com.processing.common.dto.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.Instant;

@ControllerAdvice
public class ApiKeyExceptionHandler {
    @ExceptionHandler
    public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException e) {
        var errorResponse = new ErrorResponse(
                "Bad Request",
                "Role doesn't exist",
                Instant.now().toString(),
                "keyManagement",
                ""
        );

        return ResponseEntity.badRequest().body(errorResponse);
    }
}

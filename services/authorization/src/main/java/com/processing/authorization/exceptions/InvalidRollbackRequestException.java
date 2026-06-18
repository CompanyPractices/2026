package com.processing.authorization.exceptions;

public class InvalidRollbackRequestException extends RuntimeException {
    public InvalidRollbackRequestException(String message) {
        super(message);
    }
}

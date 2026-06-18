package com.processing.authorization.exceptions;

public class RollbackConflictException extends RuntimeException {
    public RollbackConflictException(String message) {
        super(message);
    }
}

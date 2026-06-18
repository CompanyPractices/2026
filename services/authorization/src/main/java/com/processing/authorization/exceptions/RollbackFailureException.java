package com.processing.authorization.exceptions;

public class RollbackFailureException extends RuntimeException {
    public RollbackFailureException(String message) {
        super(message);
    }
}

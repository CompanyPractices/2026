package com.processing.authorization.exceptions;

public class InvalidGetCardRequestException extends RuntimeException {
    public InvalidGetCardRequestException(String message) {
        super(message);
    }
}

package com.processing.exceptions;

public abstract class CardManagementException extends RuntimeException {

    public CardManagementException(String message) {
        super(message);
    }
}

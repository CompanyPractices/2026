package com.processing.cardmanagement.exceptions;

public class OutboxDeserializationException extends RuntimeException {
    public OutboxDeserializationException(String eventType) {
        super("Failed to deserialize event: " + eventType);
    }
}

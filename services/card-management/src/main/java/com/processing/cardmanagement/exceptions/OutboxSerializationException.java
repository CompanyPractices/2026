package com.processing.cardmanagement.exceptions;

public class OutboxSerializationException extends RuntimeException {
    public OutboxSerializationException(String eventType) {
        super("Failed to serialize event: " + eventType);
    }
}

package com.processing.cardmanagement.exceptions;

import java.util.UUID;

public class OutboxEventNotFoundException extends RuntimeException {
    public OutboxEventNotFoundException(UUID id) {
        super("outbox event not found: " + id);
    }
}

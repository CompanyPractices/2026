package com.processing.cardmanagement.models;

import com.processing.cardmanagement.events.CardOutboxEvent;
import jakarta.annotation.Nullable;

import java.time.Instant;
import java.util.UUID;

public record CardOutboxEventData(
    UUID id,
    CardOutboxEvent event,
    Instant createdAt,
    @Nullable Instant processedAt,
    int retryCount,
    @Nullable String lastError,
    OutboxEventDataStatus status
) {

    public CardOutboxEventData(CardOutboxEvent event) {
        this(
            UUID.randomUUID(),
            event,
            Instant.now(),
            null,
            0,
            null,
            OutboxEventDataStatus.PENDING
        );
    }

    public CardOutboxEventData processed() {
        return new CardOutboxEventData(
            id,
            event,
            createdAt,
            Instant.now(),
            retryCount,
            lastError,
            OutboxEventDataStatus.PROCESSED
        );
    }

    public CardOutboxEventData withRetry(String error) {
        return new CardOutboxEventData(
            id,
            event,
            createdAt,
            processedAt,
            retryCount + 1,
            error,
            status
        );
    }

    public CardOutboxEventData failed() {
        return new CardOutboxEventData(
            id,
            event,
            createdAt,
            processedAt,
            retryCount,
            lastError,
            OutboxEventDataStatus.FAILED
        );
    }
}

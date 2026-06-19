package com.processing.cardmanagement.events;

public sealed interface CardEvent permits
        CardsBatchGeneratedEvent,
        CardServiceCreationEvent,
        CardServiceDeletionEvent,
        CardServicePatchEvent,
        CardServiceReserveEvent,
        CardServiceRollbackEvent,
        CardServiceBulkUpdateEvent {
}

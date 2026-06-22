package com.processing.cardmanagement.events;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = CardsBatchGeneratedEvent.class, name = "CardsBatchGeneratedEvent"),
        @JsonSubTypes.Type(value = CardServiceCreationEvent.class, name = "CardServiceCreationEvent"),
        @JsonSubTypes.Type(value = CardServiceDeletionEvent.class, name = "CardServiceDeletionEvent"),
        @JsonSubTypes.Type(value = CardServicePatchEvent.class, name = "CardServicePatchEvent"),
        @JsonSubTypes.Type(value = CardServiceReserveEvent.class, name = "CardServiceReserveEvent"),
        @JsonSubTypes.Type(value = CardServiceRollbackEvent.class, name = "CardServiceRollbackEvent")
})
public sealed interface CardEvent permits
        CardsBatchGeneratedEvent,
        CardServiceCreationEvent,
        CardServiceDeletionEvent,
        CardServicePatchEvent,
        CardServiceReserveEvent,
        CardServiceRollbackEvent,
        CardServiceBulkUpdateEvent {
}

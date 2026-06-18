package com.processing.cardmanagement.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.processing.cardmanagement.exceptions.OutboxSerializationException;
import com.processing.cardmanagement.models.EventStatus;
import com.processing.cardmanagement.models.OutboxEventEntity;
import com.processing.cardmanagement.repositories.OutboxRepository;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@RequiredArgsConstructor
public class CardEventNotifier {

    private final OutboxRepository outboxRepository;
    private final ObjectMapper mapper;

    public void onEvent(CardEvent event) {
        try {
            String eventName = event.getClass().getName();
            String payload = mapper.writeValueAsString(event);

            outboxRepository.save(new OutboxEventEntity(
                    UUID.randomUUID(),
                    eventName,
                    payload,
                    Instant.now(),
                    null,
                    0,
                    null,
                    EventStatus.PENDING.toString()
            ));
        } catch (JsonProcessingException e) {
            throw new OutboxSerializationException(event.getClass().getSimpleName());
        }
    }
}

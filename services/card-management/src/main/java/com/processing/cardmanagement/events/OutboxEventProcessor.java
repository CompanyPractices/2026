package com.processing.cardmanagement.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.processing.cardmanagement.exceptions.OutboxDeserializationException;
import com.processing.cardmanagement.models.EventStatus;
import com.processing.cardmanagement.models.OutboxEventEntity;
import com.processing.cardmanagement.options.OutboxOptions;
import com.processing.cardmanagement.repositories.OutboxRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventProcessor {

    private final OutboxRepository outboxRepository;
    private final List<CardEventListener> eventListeners;
    private final ObjectMapper objectMapper;
    private final OutboxOptions outboxOptions;

    @Transactional
    public void processSingleEvent(OutboxEventEntity outboxEvent) {
        try {
            CardEvent cardEvent = deserialize(outboxEvent);
            eventListeners.forEach(l -> l.onEvent(cardEvent));
            outboxEvent.setStatus(EventStatus.PROCESSED);
            outboxEvent.setProcessedAt(Instant.now());
        } catch (Exception e) {
            log.error("processing failed for event {}: {}", outboxEvent.getId(), e.getMessage());
            handleFail(outboxEvent, e);
        }
        outboxRepository.save(outboxEvent);
    }

    private CardEvent deserialize(OutboxEventEntity event) {
        try {
            return objectMapper.readValue(event.getPayload(), CardEvent.class);
        } catch (JsonProcessingException e) {
            throw new OutboxDeserializationException(event.getEventType());
        }
    }

    private void handleFail(OutboxEventEntity event, Exception ex) {
        event.setRetryCount(event.getRetryCount() + 1);
        event.setLastError(ex.getMessage());

        if (event.getRetryCount() >= outboxOptions.maxRetryCount()) {
            event.setStatus(EventStatus.FAILED);
        }
    }
}

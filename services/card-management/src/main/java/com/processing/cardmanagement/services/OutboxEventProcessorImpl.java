package com.processing.cardmanagement.services;

import com.processing.cardmanagement.events.CardEventListener;
import com.processing.cardmanagement.models.CardOutboxEventData;
import com.processing.cardmanagement.options.OutboxOptions;
import com.processing.cardmanagement.repositories.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class OutboxEventProcessorImpl implements OutboxEventProcessor {

    private final OutboxRepository outboxRepository;
    private final List<CardEventListener> eventListeners;
    private final OutboxOptions outboxOptions;

    @Override
    public CardOutboxEventData save(CardOutboxEventData data) {
        return outboxRepository.save(data);
    }

    @Override
    public void processSingleEvent(CardOutboxEventData outboxEventData) {
        try {
            var event = outboxEventData.event();
            eventListeners.forEach(l -> l.onEvent(event));
            outboxEventData = outboxEventData.processed();
        } catch (Exception e) {
            log.error("processing failed for event {}: {}", outboxEventData.id(), e.getMessage());
            outboxEventData = handleFail(outboxEventData, e);
        }
        outboxRepository.save(outboxEventData);
    }

    private CardOutboxEventData handleFail(CardOutboxEventData eventData, Exception ex) {
        eventData = eventData.withRetry(ex.getMessage());
        if (eventData.retryCount() >= outboxOptions.maxRetryCount()) {
            eventData = eventData.failed();
        }
        return eventData;
    }
}

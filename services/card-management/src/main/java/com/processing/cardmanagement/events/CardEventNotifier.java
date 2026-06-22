package com.processing.cardmanagement.events;

import com.processing.cardmanagement.models.CardOutboxEventData;
import com.processing.cardmanagement.services.OutboxEventProcessor;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class CardEventNotifier {

    private final OutboxEventProcessor outboxEventProcessor;
    private final List<CardEventListener> eventListeners;

    public void onEvent(CardEvent event) {
        if (event instanceof CardOutboxEvent outboxEvent) {
            outboxEventProcessor.save(new CardOutboxEventData(outboxEvent));
        } else {
            eventListeners.forEach(el -> el.onEvent(event));
        }
    }
}

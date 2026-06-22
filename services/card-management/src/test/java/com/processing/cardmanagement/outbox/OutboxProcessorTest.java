package com.processing.cardmanagement.outbox;

import com.processing.cardmanagement.events.CardEventListener;
import com.processing.cardmanagement.events.CardServiceCreationEvent;
import com.processing.cardmanagement.models.CardOutboxEventData;
import com.processing.cardmanagement.models.OutboxEventDataStatus;
import com.processing.cardmanagement.options.OutboxOptions;
import com.processing.cardmanagement.repositories.OutboxRepository;
import com.processing.cardmanagement.services.OutboxEventProcessorImpl;
import com.processing.cardmanagement.services.OutboxProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OutboxProcessorTest {

    @Mock
    private OutboxRepository outboxRepository;

    @Mock
    private CardEventListener listener;

    private final OutboxOptions outboxOptions = new OutboxOptions(1000, 3);

    private final ArgumentCaptor<CardOutboxEventData> eventDataCaptor =
        ArgumentCaptor.forClass(CardOutboxEventData.class);

    private OutboxProcessor outboxProcessor;

    @BeforeEach
    void setUp() {
        OutboxEventProcessorImpl eventProcessor = new OutboxEventProcessorImpl(
            outboxRepository,
            List.of(listener),
            outboxOptions
        );
        outboxProcessor = new OutboxProcessor(
            outboxRepository, eventProcessor, outboxOptions);
    }

    @Test
    void shouldProcessPendingEvent() {
        CardServiceCreationEvent cardEvent = new CardServiceCreationEvent(1);

        CardOutboxEventData outboxEvent = new CardOutboxEventData(cardEvent);
        when(outboxRepository.findPending(outboxOptions.maxRetryCount()))
            .thenReturn(List.of(outboxEvent));
        outboxProcessor.process();
        verify(outboxRepository, times(1)).save(eventDataCaptor.capture());
        var updatedEvent = eventDataCaptor.getValue();

        assertEquals(OutboxEventDataStatus.PROCESSED, updatedEvent.status());
        assertNotNull(updatedEvent.processedAt());
        verify(listener, times(1)).onEvent(any(CardServiceCreationEvent.class));
    }

    @Test
    void shouldIncrementRetryCountOnFail() {
        CardServiceCreationEvent cardEvent = new CardServiceCreationEvent(1);

        CardOutboxEventData outboxEvent = new CardOutboxEventData(cardEvent);
        when(outboxRepository.findPending(outboxOptions.maxRetryCount()))
            .thenReturn(List.of(outboxEvent));
        doThrow(new RuntimeException("listener failed")).when(listener).onEvent(any());
        outboxProcessor.process();
        verify(outboxRepository, times(1)).save(eventDataCaptor.capture());
        var updatedEvent = eventDataCaptor.getValue();

        assertEquals(1, updatedEvent.retryCount());
        assertEquals(OutboxEventDataStatus.PENDING, updatedEvent.status());
        assertNotNull(updatedEvent.lastError());
    }

    @Test
    void shouldSetFailedStatusAfterMaxRetries() {
        CardServiceCreationEvent cardEvent = new CardServiceCreationEvent(1);

        CardOutboxEventData outboxEvent = new CardOutboxEventData(cardEvent);
        doThrow(new RuntimeException("listener failed")).when(listener).onEvent(any());

        for (int i = 0; i < outboxOptions.maxRetryCount(); ++i) {
            when(outboxRepository.findPending(outboxOptions.maxRetryCount()))
                .thenReturn(List.of(outboxEvent));
            outboxProcessor.process();
            outboxEvent = Mockito.mockingDetails(outboxRepository)
                .getInvocations()
                .stream()
                .filter(inv -> inv.getMethod().getName().equals("save"))
                .map(inv -> (CardOutboxEventData) inv.getArguments()[0])
                .reduce((first, second) -> second)
                .orElse(outboxEvent);
        }
        verify(outboxRepository, times(outboxOptions.maxRetryCount())).save(eventDataCaptor.capture());
        CardOutboxEventData finalEvent = eventDataCaptor.getAllValues().getLast();
        assertEquals(outboxOptions.maxRetryCount(), finalEvent.retryCount());
        assertEquals(OutboxEventDataStatus.FAILED, finalEvent.status());
        assertNotNull(finalEvent.lastError());
    }

    @Test
    void shouldSkipFailedEvents() {
        when(outboxRepository.findPending(outboxOptions.maxRetryCount())).thenReturn(List.of());
        outboxProcessor.process();
        verify(listener, times(0)).onEvent(any());
    }
}

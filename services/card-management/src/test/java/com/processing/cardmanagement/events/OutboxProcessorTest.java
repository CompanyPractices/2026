package com.processing.cardmanagement.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.processing.cardmanagement.models.EventStatus;
import com.processing.cardmanagement.models.OutboxEventEntity;
import com.processing.cardmanagement.options.OutboxOptions;
import com.processing.cardmanagement.repositories.OutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OutboxProcessorTest {

    @Mock
    private OutboxRepository outboxRepository;

    @Mock
    private CardEventListener listener;

    @Mock
    private OutboxOptions outboxOptions;

    private OutboxProcessor outboxProcessor;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final int MAX_COUNT_RETRY = 3;

    @BeforeEach
    void setUp() {
        when(outboxOptions.maxRetryCount()).thenReturn(MAX_COUNT_RETRY);
        OutboxEventProcessor eventProcessor = new OutboxEventProcessor(
                outboxRepository, List.of(listener), objectMapper, outboxOptions);
        outboxProcessor = new OutboxProcessor(
                outboxRepository, eventProcessor, outboxOptions);
    }

    @Test
    void shouldProcessPendingEvent() throws Exception {
        CardServiceCreationEvent cardEvent = new CardServiceCreationEvent(1);
        String payload = objectMapper.writeValueAsString(cardEvent);

        OutboxEventEntity outboxEvent = new OutboxEventEntity(
                UUID.randomUUID(),
                "CardServiceCreationEvent",
                payload,
                Instant.now(),
                null,
                0,
                null,
                EventStatus.PENDING
        );
        when(outboxRepository.findPending(MAX_COUNT_RETRY)).thenReturn(List.of(outboxEvent));
        outboxProcessor.process();

        assertEquals(EventStatus.PROCESSED, outboxEvent.getStatus());
        assertNotNull(outboxEvent.getProcessedAt());
        verify(listener, times(1)).onEvent(any(CardServiceCreationEvent.class));
        verify(outboxRepository, times(1)).save(outboxEvent);
    }

    @Test
    void shouldIncrementRetryCountOnFail() throws Exception {
        CardServiceCreationEvent cardEvent = new CardServiceCreationEvent(1);
        String payload = objectMapper.writeValueAsString(cardEvent);

        OutboxEventEntity outboxEvent = new OutboxEventEntity(
                UUID.randomUUID(),
                "CardServiceCreationEvent",
                payload,
                Instant.now(),
                null,
                0,
                null,
                EventStatus.PENDING
        );
        when(outboxRepository.findPending(MAX_COUNT_RETRY)).thenReturn(List.of(outboxEvent));
        doThrow(new RuntimeException("listener failed")).when(listener).onEvent(any());
        outboxProcessor.process();

        assertEquals(1, outboxEvent.getRetryCount());
        assertEquals(EventStatus.PENDING, outboxEvent.getStatus());
        assertNotNull(outboxEvent.getLastError());
        verify(outboxRepository, times(1)).save(outboxEvent);
    }

    @Test
    void shouldSetFailedStatusAfterMaxRetries() throws Exception {
        CardServiceCreationEvent cardEvent = new CardServiceCreationEvent(1);
        String payload = objectMapper.writeValueAsString(cardEvent);

        OutboxEventEntity outboxEvent = new OutboxEventEntity(
                UUID.randomUUID(),
                "CardServiceCreationEvent",
                payload,
                Instant.now(),
                null,
                2,
                null,
                EventStatus.PENDING
        );
        when(outboxRepository.findPending(MAX_COUNT_RETRY)).thenReturn(List.of(outboxEvent));
        doThrow(new RuntimeException("listener failed")).when(listener).onEvent(any());
        outboxProcessor.process();

        assertEquals(3, outboxEvent.getRetryCount());
        assertEquals(EventStatus.FAILED, outboxEvent.getStatus());
        assertNotNull(outboxEvent.getLastError());
        verify(outboxRepository, times(1)).save(outboxEvent);
    }

    @Test
    void shouldSkipFailedEvents() throws Exception {
        when(outboxRepository.findPending(MAX_COUNT_RETRY)).thenReturn(List.of());
        outboxProcessor.process();
        verify(listener, times(0)).onEvent(any());
    }

    @Test
    void shouldHandleUnknownEventType() {
        OutboxEventEntity outboxEvent = new OutboxEventEntity(
                UUID.randomUUID(),
                "UnknownError",
                "Error",
                Instant.now(),
                null,
                0,
                null,
                EventStatus.PENDING
        );

        when(outboxRepository.findPending(MAX_COUNT_RETRY)).thenReturn(List.of(outboxEvent));
        outboxProcessor.process();

        assertEquals(1, outboxEvent.getRetryCount());
        assertNotNull(outboxEvent.getLastError());
        verify(listener, times(0)).onEvent(any());
    }
}

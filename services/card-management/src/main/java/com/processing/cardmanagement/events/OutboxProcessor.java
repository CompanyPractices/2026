package com.processing.cardmanagement.events;

import com.processing.cardmanagement.models.OutboxEventEntity;
import com.processing.cardmanagement.options.OutboxOptions;
import com.processing.cardmanagement.repositories.OutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class OutboxProcessor {
    private final OutboxRepository outboxRepository;
    private final OutboxEventProcessor eventProcessor;
    private final OutboxOptions outboxOptions;

    @Scheduled(fixedDelayString = "${app.outbox.interval-ms}")
    public void process() {
        List<OutboxEventEntity> events = outboxRepository.findPending(outboxOptions.maxRetryCount());
        events.forEach(eventProcessor::processSingleEvent);
    }
}

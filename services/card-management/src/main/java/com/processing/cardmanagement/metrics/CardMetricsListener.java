package com.processing.cardmanagement.metrics;

import com.processing.cardmanagement.events.CardGeneratedEvent;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CardMetricsListener {
    private final MeterRegistry meterRegistry;

    @EventListener
    public void onCardGenerated(CardGeneratedEvent event) {
        meterRegistry.counter("cards.generated",
                "status", event.status().name()).increment();
    }
}

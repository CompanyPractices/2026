package com.processing.cardmanagement.metrics;

import com.processing.cardmanagement.events.*;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CardServiceMetricsEventListener implements CardEventListener {

    private final MeterRegistry meterRegistry;

    @Override
    public void onEvent(CardEvent event) {
        switch (event) {
            case CardServiceCreationEvent e -> meterRegistry
                .counter("cards.operations", "type", "created")
                .increment(e.amount());

            case CardServicePatchEvent ignored -> meterRegistry
                .counter("cards.operations", "type", "patched")
                .increment();

            case CardServiceDeletionEvent ignored -> meterRegistry
                .counter("cards.operations", "type", "deleted")
                .increment();

            case CardServiceReserveEvent ignored -> meterRegistry
                .counter("cards.operations", "type", "reserved")
                .increment();

            case CardGeneratedEvent e -> meterRegistry
                .counter("cards.generated", "status", e.status().name())
                .increment();
        }
    }
}

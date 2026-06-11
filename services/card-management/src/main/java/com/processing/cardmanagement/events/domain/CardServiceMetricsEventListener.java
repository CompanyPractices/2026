package com.processing.cardmanagement.events.domain;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CardServiceMetricsEventListener implements CardServiceEventListener {

    private final MeterRegistry meterRegistry;

    @Override
    public void onEvent(CardServiceEvent event) {
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

            case CardServiceReserveEvent e -> meterRegistry
                .counter("cards.operations", "type", "reserved")
                .increment();
        }
    }
}

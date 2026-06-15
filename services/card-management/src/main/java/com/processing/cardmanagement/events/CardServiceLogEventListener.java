package com.processing.cardmanagement.events;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CardServiceLogEventListener implements CardEventListener {

    @Override
    public void onEvent(CardEvent event) {
        switch (event) {
            case CardServiceCreationEvent e -> log.info("Created {} cards", e.amount());
            case CardServicePatchEvent e -> log.info("Patched card {}", e.pan());
            case CardServiceDeletionEvent e -> log.info("Deleted card {}", e.pan());
            case CardServiceReserveEvent e -> log.info("Reserved {} from card {}", e.amount(), e.pan());
            default -> {
            }
        }
    }
}

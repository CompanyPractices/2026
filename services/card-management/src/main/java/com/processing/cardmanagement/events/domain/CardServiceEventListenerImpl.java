package com.processing.cardmanagement.events.domain;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Primary
@RequiredArgsConstructor
public class CardServiceEventListenerImpl implements CardServiceEventListener {

    private final List<CardServiceEventListener> listeners;

    @Override
    public void onEvent(CardServiceEvent event) {
        for (var l : listeners) {
            l.onEvent(event);
        }
    }
}

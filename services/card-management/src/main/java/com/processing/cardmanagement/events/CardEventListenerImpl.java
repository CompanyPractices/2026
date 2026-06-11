package com.processing.cardmanagement.events;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Primary
@RequiredArgsConstructor
public class CardEventListenerImpl implements CardEventListener {

    private final List<CardEventListener> listeners;

    @Override
    public void onEvent(CardEvent event) {
        for (var l : listeners) {
            l.onEvent(event);
        }
    }
}

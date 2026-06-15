package com.processing.cardmanagement.events;

import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class CardEventNotifier {

    private final List<CardEventListener> listeners;

    public void onEvent(CardEvent event) {
        for (var l : listeners) {
            l.onEvent(event);
        }
    }
}

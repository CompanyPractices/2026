package com.processing.cardmanagement.events;

import com.processing.cardmanagement.models.CardStatus;

public record CardGeneratedEvent(CardStatus status) implements CardEvent {}

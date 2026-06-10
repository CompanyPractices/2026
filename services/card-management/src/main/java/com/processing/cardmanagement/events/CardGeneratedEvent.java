package com.processing.cardmanagement.events;

import com.processing.common.dto.cardmanagement.CardStatus;

public record CardGeneratedEvent(CardStatus status) {
}

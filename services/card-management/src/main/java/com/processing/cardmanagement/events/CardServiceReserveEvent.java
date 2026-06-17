package com.processing.cardmanagement.events;

import java.math.BigDecimal;

public record CardServiceReserveEvent(String pan, BigDecimal amount) implements CardEvent {}

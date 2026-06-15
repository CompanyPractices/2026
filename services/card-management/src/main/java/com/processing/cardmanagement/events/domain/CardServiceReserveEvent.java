package com.processing.cardmanagement.events.domain;

import java.math.BigDecimal;

public record CardServiceReserveEvent(String pan, BigDecimal amount) implements CardServiceEvent {}

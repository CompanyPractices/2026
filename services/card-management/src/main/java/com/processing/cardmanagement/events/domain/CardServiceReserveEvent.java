package com.processing.cardmanagement.events.domain;

public record CardServiceReserveEvent(String pan, long amount) implements CardServiceEvent {}

package com.processing.cardmanagement.events.domain;

public record CardServiceCreationEvent(long amount) implements CardServiceEvent {}

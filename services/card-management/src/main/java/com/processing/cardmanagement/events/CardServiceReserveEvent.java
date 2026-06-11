package com.processing.cardmanagement.events;

public record CardServiceReserveEvent(String pan, long amount) implements CardEvent {}

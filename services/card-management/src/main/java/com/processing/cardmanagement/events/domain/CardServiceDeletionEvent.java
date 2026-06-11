package com.processing.cardmanagement.events.domain;

public record CardServiceDeletionEvent(String pan) implements CardServiceEvent {}

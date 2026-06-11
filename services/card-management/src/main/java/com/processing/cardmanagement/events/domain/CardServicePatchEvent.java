package com.processing.cardmanagement.events.domain;

public record CardServicePatchEvent(String pan) implements CardServiceEvent {}

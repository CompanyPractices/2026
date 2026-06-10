package com.processing.cardmanagement.events.domain;

public sealed interface CardServiceEvent permits
    CardServiceCreationEvent,
    CardServiceDeletionEvent,
    CardServicePatchEvent,
    CardServiceReserveEvent {}

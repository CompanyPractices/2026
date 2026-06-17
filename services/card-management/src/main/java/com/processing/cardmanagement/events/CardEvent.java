package com.processing.cardmanagement.events;

public sealed interface CardEvent permits CardGeneratedEvent, CardServiceCreationEvent, CardServiceDeletionEvent, CardServicePatchEvent, CardServiceReserveEvent, CardServiceRollbackEvent {}

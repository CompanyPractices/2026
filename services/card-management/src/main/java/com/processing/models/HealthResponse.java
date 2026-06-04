package com.processing.models;

public record HealthResponse(
    String status,
    String service,
    long cardsInDatabase
) {}

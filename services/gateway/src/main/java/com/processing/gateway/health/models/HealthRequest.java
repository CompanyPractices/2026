package com.processing.gateway.health.models;

public record HealthRequest(
        String uri,
        String service
) {}

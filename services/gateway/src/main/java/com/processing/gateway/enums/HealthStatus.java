package com.processing.gateway.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum HealthStatus {
    OK,
    UNAVAILABLE,
    DEGRADED;

    @JsonValue
    @Override
    public String toString() {
        return super.name().toLowerCase();
    }
}

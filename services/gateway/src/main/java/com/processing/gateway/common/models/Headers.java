package com.processing.gateway.common.models;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum Headers {
    X_REQUEST_ID("X-Request-ID"),
    X_CACHE("X-Cache"),
    X_REAL_IP("X-Real-IP"),
    X_FORWARDED_FOR("X-Forwarded-For");

    private final String value;

    @RequiredArgsConstructor
    @Getter
    public enum Values {
        CACHE_HIT("HIT"),
        CACHE_MISS("MISS");

        private final String value;
    }
}

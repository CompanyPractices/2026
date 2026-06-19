package com.processing.gateway.common.models;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum Routes {
    API_CARDS("/api/cards"),
    INTERNAL("/internal"),
    API_TRANSACTIONS("/api/transactions"),
    API_INTERNAL_ROUTE("/api/internal/route");

    private final String value;
}

package com.processing.dto;

import java.util.List;

public record RunResponse(
    int totalSubmitted,
    int approved,
    int declined,
    long elapsedMs,
    List<AuthorizationResponse> transactions
) {}

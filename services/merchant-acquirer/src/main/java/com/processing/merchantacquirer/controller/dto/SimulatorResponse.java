package com.processing.merchantacquirer.controller.dto;

import com.processing.merchantacquirer.domain.model.AuthorizationResponse;
import java.util.List;

public record SimulatorResponse(
    int totalSubmitted,
    int approved,
    int declined,
    int elapsedMs,
    List<AuthorizationResponse> transactions) {}

package com.processing.merchantacquirer.dto;

import java.util.List;

public record SimulatorResponse(int totalSubmitted,
                                int approved,
                                int declined,
                                int elapsedMs,
                                List<AuthorizationResponse> transactions) {
}

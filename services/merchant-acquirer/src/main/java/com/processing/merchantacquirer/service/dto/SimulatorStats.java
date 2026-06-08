package com.processing.merchantacquirer.service.dto;

import com.processing.merchantacquirer.domain.model.AuthorizationResponse;
import java.util.List;

public record SimulatorStats(List<AuthorizationResponse> responses, int approved, int declined) {}

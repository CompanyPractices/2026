package com.processing.merchantacquirer.controller.dto;

import com.processing.merchantacquirer.domain.entity.ScenarioType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record SimulatorRequest(
    @Min(1) int count,
    @NotNull(message = "Scenario is required") ScenarioType scenario,
    List<String> mccCodes) {}

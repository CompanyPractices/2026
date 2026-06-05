package com.processing.terminalsimulator.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record RunRequest (
    @NotNull
    @Min(1)
    Integer count,
    @NotNull
    String scenario
) {}

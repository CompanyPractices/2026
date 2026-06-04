package com.processing.models;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record GenerateCardsRequest (
    @Min(1) int count,
    @NotEmpty List<String> bins
) {}

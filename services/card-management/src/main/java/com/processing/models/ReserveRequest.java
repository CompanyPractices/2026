package com.processing.models;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ReserveRequest(

    @NotNull
    @Positive
    long amount,

    @NotBlank
    String rrn
) {}

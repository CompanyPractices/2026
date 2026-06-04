package com.processing.models;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "Request to reserve funds on a card")
public record ReserveRequest(

    @NotNull
    @Positive
    @Schema(description = "Amount to reserve in kopecks", example = "150000")
    long amount,

    @NotBlank
    @Schema(description = "Retrieval Reference Number", example = "012345678901")
    String rrn
) {}

package com.processing.common.dto.cardmanagement;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

@Schema(description = "Request to reserve funds on a card")
public record ReserveRequest(

    @Positive
    @Schema(description = "Amount to reserve in kopecks", example = "150000")
    BigDecimal amount,

    @NotBlank
    @Schema(description = "Retrieval Reference Number", example = "012345678901")
    String rrn
) {}

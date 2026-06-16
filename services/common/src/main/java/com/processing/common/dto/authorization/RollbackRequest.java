package com.processing.common.dto.authorization;

import com.processing.common.dto.annotations.ExactSize;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record RollbackRequest(
    @NotBlank
    @ExactSize(12)
    @Schema(description = "Retrieval reference number", example = "012345678901")
    String rrn,

    @NotBlank
    @ExactSize(16)
    @Schema(description = "Card number", example = "4000001234560001")
    String pan,

    @NotNull
    @Positive
    @Schema(description = "Rollback amount", example = "1000")
    BigDecimal amount
) { }

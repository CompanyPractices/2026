package com.processing.common.dto.terminalsimulator;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record TerminalStartContinuousRequest (
    @NotNull
    @Min(1)
    int tps,
    @NotNull
    TransactionType transactionType
) {}

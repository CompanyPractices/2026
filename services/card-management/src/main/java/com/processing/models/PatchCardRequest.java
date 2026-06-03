package com.processing.models;

import com.processing.annotations.NotNegative;
import jakarta.annotation.Nullable;

public record PatchCardRequest(

    @Nullable
    CardEntity.Status status,

    @Nullable
    @NotNegative
    Integer dailyLimit,

    @Nullable
    @NotNegative
    Integer monthlyLimit,

    @Nullable
    Integer availableBalance
) {}

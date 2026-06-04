package com.processing.cardmanagement.models;

import com.processing.cardmanagement.annotations.NotNegative;
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

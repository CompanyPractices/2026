package com.processing.cardmanagement.models;

import com.processing.cardmanagement.annotations.Bin;
import com.processing.cardmanagement.annotations.NotNegative;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record GetCardsRequest(

    @Positive
    Integer limit,

    @NotNegative
    Integer offset,

    @Nullable
    CardEntity.Status status,

    @Nullable
    @Bin
    String bin,

    @Nullable
    @Size(min = 1, max = 10)
    @Pattern(regexp = "^[A-Z0-9]+$")
    String issuerId,

    @Nullable
    LocalDateTime startDate,

    @Nullable
    LocalDateTime endDate
) {

    public GetCardsRequest {
        if (limit == null) {
            limit = 10;
        }
        if (offset == null) {
            offset = 0;
        }
    }
}

package com.processing.models;

import com.processing.annotations.Bin;
import com.processing.annotations.NotNegative;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

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
    LocalDate startDate,

    @Nullable
    LocalDate endDate
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

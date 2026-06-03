package com.processing.models;

import com.processing.annotations.DigitsOnly;
import com.processing.annotations.NotNegative;
import com.processing.annotations.Pan;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record GetCardsRequest(

    @Positive
    Integer limit,

    @NotNegative
    Integer offset,

    @Nullable
    @Pan
    String pan,

    @Nullable
    @Size(min = 1, max = 10)
    @DigitsOnly
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

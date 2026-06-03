package com.processing.models;

import com.processing.annotations.DigitsOnly;
import com.processing.annotations.Pan;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record GetCardsRequest(

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
) {}

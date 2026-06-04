package com.processing.cardmanagement.models;

import com.processing.cardmanagement.annotations.Bin;
import com.processing.cardmanagement.annotations.NotNegative;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

@Schema(description = "Request parameters for getting cards list")
public record GetCardsRequest(

    @Positive
    @Schema(description = "Number of cards per page", example = "10")
    Integer limit,

    @NotNegative
    @Schema(description = "Offset for pagination", example = "0")
    Integer offset,

    @Nullable
    @Schema(description = "Card status", example = "ACTIVE", allowableValues = {"ACTIVE", "INACTIVE", "BLOCKED", "EXPIRED", "DELETED"})
    CardEntity.Status status,

    @Nullable
    @Bin
    @Schema(description = "Bank Identification Number (BIN)", example = "400000")
    String bin,

    @Nullable
    @Size(min = 1, max = 10)
    @Pattern(regexp = "^[A-Z0-9]+$")
    @Schema(description = "Issuer ID", example = "ZZZZZZ")
    String issuerId,

    @Nullable
    @Schema(description = "Start date")
    LocalDateTime startDate,

    @Nullable
    @Schema(description = "End date")
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

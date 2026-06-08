package com.processing.common.dto.cardmanagement;
import com.processing.common.dto.annotations.NotNegative;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;

@Schema(description = "Request to update a card")
public record PatchCardRequest(

    @Nullable
    @Schema(
        description = "Card status",
        example = "ACTIVE",
        allowableValues = {"ACTIVE",
        "INACTIVE",
        "BLOCKED",
        "EXPIRED",
        "DELETED"}
    )
    CardStatus status,

    @Nullable
    @NotNegative
    @Schema(description = "Daily limit in kopecks", example = "15000000")
    Integer dailyLimit,

    @Nullable
    @NotNegative
    @Schema(description = "Monthly limit in kopecks", example = "300000000")
    Integer monthlyLimit,

    @Nullable
    @Schema(description = "Available balance in kopecks", example = "1000000000")
    Integer availableBalance
) {}

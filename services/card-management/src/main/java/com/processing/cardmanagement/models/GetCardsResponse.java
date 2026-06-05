package com.processing.cardmanagement.models;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record GetCardsResponse(
        @Schema(description = "Total number of cards matching filters", example = "100")
        int total,

        @Schema(description = "List of cards")
        List<CardDto> cards) {
}

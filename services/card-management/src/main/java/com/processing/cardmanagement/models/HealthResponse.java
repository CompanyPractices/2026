package com.processing.cardmanagement.models;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO о состоянии сервиса на текущий момент
 *
 * @param status          статус
 * @param service         название сервиса
 * @param cardsInDatabase количество карт в базе данных
 */
@Schema(description = "Health check response")
public record HealthResponse(
    @Schema(description = "Service status", example = "ok")
    String status,
    @Schema(description = "Service name", example = "card-management")
    String service,
    @Schema(description = "Number of cards in database", example = "1000")
    long cardsInDatabase
) {}

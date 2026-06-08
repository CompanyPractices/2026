package com.processing.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Ответ после сохранения новой транзакции")
public record TransactionStoredResponse(
        @Schema(description = "Идентификатор сохраненной транзакции")
        UUID id,
        @Schema(description = "Статус результата сохранения")
        String status
) {
}

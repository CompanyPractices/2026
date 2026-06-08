package com.processing.common.dto.transactionlogger;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Данные транзакции, полученные от Switch")
public record TransactionRequest(
        @Schema(description = "Идентификатор транзакции, сгенерированный Switch")
        @NotNull
        UUID id,

        @Schema(description = "Тип ISO 8583 сообщения")
        @NotBlank
        @Size(max = 4)
        String mti,

        @Schema(description = "Системный трассировочный номер операции")
        @NotBlank
        @Size(max = 6)
        String stan,

        @Schema(description = "Ссылочный номер транзакции (RRN), сгенерированный сервисом авторизации")
        @Size(max = 12)
        String rrn,

        @Schema(description = "Номер тестовой или замаскированной карты (PAN)")
        @NotBlank
        @Size(max = 16)
        String pan,

        @Schema(description = "Код обработки транзакции")
        @NotBlank
        @Size(max = 6)
        String processingCode,

        @Schema(description = "Сумма транзакции в минимальных единицах валюты")
        @NotNull
        @Positive
        Long amount,

        @Schema(description = "Числовой код валюты по ISO 4217")
        @NotBlank
        @Size(max = 3)
        String currencyCode,

        @Schema(description = "Идентификатор терминала")
        @NotBlank
        @Size(max = 8)
        String terminalId,

        @Schema(description = "Идентификатор мерчанта")
        @NotBlank
        @Size(max = 15)
        String merchantId,

        @Schema(description = "Код категории мерчанта")
        @NotBlank
        @Size(max = 4)
        String mcc,

        @Schema(description = "Идентификатор эквайера")
        @NotBlank
        @Size(max = 10)
        String acquirerId,

        @Schema(description = "Идентификатор эмитента")
        @Size(max = 10)
        String issuerId,

        @Schema(description = "Комиссия эквайринга в минимальных единицах валюты")
        Long acquiringFee,

        @Schema(description = "Статус результата авторизации")
        @NotNull
        TransactionStatus status,

        @Schema(description = "Причина отказа для отклоненной транзакции")
        @Size(max = 100)
        String declineReason,

        @Schema(description = "Код авторизации")
        @Size(max = 6)
        String authCode,

        @Schema(description = "Время обработки транзакции в миллисекундах")
        @PositiveOrZero
        Integer processingTimeMs,

        @Schema(description = "Дата и время передачи транзакции")
        @NotNull
        Instant transmissionDateTime,

        @Schema(description = "Дата и время создания транзакции, переданные Switch")
        @NotNull
        Instant createdAt
) {
}

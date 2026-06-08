package com.processing.dto;

import com.processing.enums.TransactionStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Сохраненная транзакция, возвращаемая при идемпотентном повторном запросе")
public record TransactionResponse(
        @Schema(description = "Идентификатор транзакции")
        UUID id,
        @Schema(description = "Тип ISO 8583 сообщения")
        String mti,
        @Schema(description = "Системный трассировочный номер операции")
        String stan,
        @Schema(description = "Ссылочный номер транзакции (RRN)")
        String rrn,
        @Schema(description = "Номер тестовой или замаскированной карты (PAN)")
        String pan,
        @Schema(description = "Код обработки транзакции")
        String processingCode,
        @Schema(description = "Сумма транзакции в минимальных единицах валюты")
        Long amount,
        @Schema(description = "Числовой код валюты по ISO 4217")
        String currencyCode,
        @Schema(description = "Идентификатор терминала")
        String terminalId,
        @Schema(description = "Идентификатор мерчанта")
        String merchantId,
        @Schema(description = "Код категории мерчанта")
        String mcc,
        @Schema(description = "Идентификатор эквайера")
        String acquirerId,
        @Schema(description = "Идентификатор эмитента")
        String issuerId,
        @Schema(description = "Комиссия эквайринга в минимальных единицах валюты")
        Long acquiringFee,
        @Schema(description = "Статус результата авторизации")
        TransactionStatus status,
        @Schema(description = "Причина отказа для отклоненной транзакции")
        String declineReason,
        @Schema(description = "Код авторизации")
        String authCode,
        @Schema(description = "Время обработки транзакции в миллисекундах")
        Integer processingTimeMs,
        @Schema(description = "Дата и время передачи транзакции")
        Instant transmissionDateTime,
        @Schema(description = "Дата и время создания транзакции")
        Instant createdAt
) {
}

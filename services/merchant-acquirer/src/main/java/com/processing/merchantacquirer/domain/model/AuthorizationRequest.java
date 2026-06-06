package com.processing.merchantacquirer.domain.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@Builder
public class AuthorizationRequest {
    @NotNull
    private String mti; // "0100" = Auth Request, всегда одинаковый
    @NotNull
    private String stan; // System Trace Audit Number, написать реализацию генерации
    @NotNull
    private String pan; //card
    @NotNull
    private String processingCode; //"000000" = покупка
    @NotBlank
    private int amount; //Сумма в копейках
    @NotNull
    private String currencyCode; //"643" - идентификатор валюты(рубли), есть в CardsResponse
    @NotNull
    private LocalDateTime transmissionDateTime; // Время отправки
    @NotNull
    private String terminalId; // terminal ?
    private String terminalType; // terminal ?
    @NotNull
    private String merchantId; // merchant
    @NotNull
    private String mcc; // merchant
    private String acquirerId; // merchant
}

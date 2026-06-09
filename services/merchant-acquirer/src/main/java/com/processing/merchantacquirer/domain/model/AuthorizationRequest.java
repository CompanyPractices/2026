//package com.processing.merchantacquirer.domain.model;
//
//import lombok.Builder;
//
//import java.math.BigDecimal;
//
//@Builder
//public record AuthorizationRequest(
//        String mti, // "0100" = Auth Request, всегда одинаковый
//        String stan, // System Trace Audit Number, написать реализацию генерации
//        String pan, // card
//        String processingCode, // "000000" = покупка
//        BigDecimal amount, // Сумма в копейках
//        String currencyCode, // "643" - идентификатор валюты (рубли), есть в CardsResponse
//        String transmissionDateTime, // Время отправки
//        String terminalId, // terminal ?
//        String terminalType, // terminal ?
//        String merchantId, // merchant
//        String mcc, // merchant
//        String acquirerId // merchant
//    ) {}

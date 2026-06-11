package com.processing.cardmanagement.options;

/**
 * Настройки сервиса управления картами
 *
 * @param issuerId           номер банка эмитента
 * @param cardValidityPeriod срок действия карты
 */
public record CardServiceSettings(
    String issuerId,
    int cardValidityPeriod
) {}

package com.processing.cardmanagement.options;

/**
 * Настройки сервиса управления картами
 *
 * @param issuerId номер банка эмитента
 * @param cardYtl  срок действия карты
 */
public record CardServiceSettings(
    String issuerId,
    int cardYtl
) {}

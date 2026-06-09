package com.processing.cardmanagement.options;

/**
 * Значения по умолчанию для CardService
 *
 * @param pageLimit    значение лимита для пагинации
 * @param pageOffset   значение сдвига для пагинации
 * @param currencyCode код валюты
 * @param dailyLimit   дневной лимит карты
 * @param monthlyLimit месячный лимит карты
 * @param balance      баланс карты
 */
public record CardServiceDefaults(
    long pageLimit,
    long pageOffset,
    String currencyCode,
    long dailyLimit,
    long monthlyLimit,
    long balance
) {}

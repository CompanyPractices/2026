package com.processing.cardmanagement.models;

import com.processing.common.dto.cardmanagement.CardStatus;

/**
 * Черновик карты
 *
 * @param bin            BIN номер карты
 * @param cardholderName имя держателя карты
 * @param status         статус карты
 * @param currencyCode   код валюты
 * @param dailyLimit     дневной лимит карты
 * @param monthlyLimit   месячный лимит карты
 */
public record CardDraft(
    String bin,
    String cardholderName,
    CardStatus status,
    String currencyCode,
    long dailyLimit,
    long monthlyLimit,
    long initialBalance
) {}

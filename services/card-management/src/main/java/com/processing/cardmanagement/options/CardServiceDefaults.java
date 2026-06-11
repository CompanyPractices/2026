package com.processing.cardmanagement.options;

/**
 * Значения по умолчанию для CardService
 */
public interface CardServiceDefaults {

    /**
     * @return значение лимита для пагинации
     */
    long pageLimit();

    /**
     * @return значение сдвига для пагинации
     */
    long pageOffset();

    /**
     * @return код валюты
     */
    String currencyCode();

    /**
     * @return дневной лимит карты
     */
    long dailyLimit();

    /**
     * @return месячный лимит карты
     */
    long monthlyLimit();

    /**
     * @return баланс карты
     */
    long balance();
}

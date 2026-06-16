package com.processing.cardmanagement.options;

/**
 * Настройки сервиса управления картами
 */
public interface CardServiceSettings {

    /**
     * @return номер банка эмитента
     */
    String issuerId();

    /**
     * @return срок действия карты
     */
    int cardValidityPeriod();
}

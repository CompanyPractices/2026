package com.processing.cardmanagement.repositories;

import com.processing.cardmanagement.models.ReservationRollback;

public interface ReservationRollbackRepository {

    /**
     * Добавляет информацию о возврате средств за платеж в БД
     * Все операции выполняются транзакционно
     *
     * @param rollback информация о возврате средств
     * @return сохраненная информация
     */
    ReservationRollback save(ReservationRollback rollback);
}

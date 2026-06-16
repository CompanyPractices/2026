package com.processing.cardmanagement.repositories;

import com.processing.cardmanagement.models.Reservation;

public interface ReservationRepository {

    /**
     * Добавляет либо изменяет удержание средств,
     * Все операции выполняются транзакционно
     *
     * @param reservation информация об удержании средств
     * @return измененное / сохраненное удержание
     */
    Reservation saveTransactional(Reservation reservation);
}

package com.processing.cardmanagement.repositories;

import com.processing.cardmanagement.models.Reservation;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import java.util.function.UnaryOperator;

@RequiredArgsConstructor
public class ReservationRepositoryPersistenceAdapter implements ReservationRepository {

    private ReservationJpaRepository jpaRepository;

    @Override
    @Transactional
    public Reservation createAndExectute(
        Reservation reservation,
        UnaryOperator<Reservation> businessLogic
    ) {
        return null;
    }
}

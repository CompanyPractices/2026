package com.processing.cardmanagement.repositories;

import com.processing.cardmanagement.models.Reservation;

import java.util.Optional;

public interface ReservationRepository {

    Reservation save(Reservation reservation);

    Optional<Reservation> findByRrn(String rrn);
}

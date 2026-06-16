package com.processing.cardmanagement.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name = "reservation_rollbacks")
@NoArgsConstructor
@AllArgsConstructor
public class ReservationRollbackEntity {

    @Id
    @Column(updatable = false, nullable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reservation_id", unique = true)
    private ReservationEntity reservation;

    @Column(length = 16, nullable = false)
    private String pan;

    long reservationAmount;

    @Column(length = 12, unique = true, nullable = false)
    String rrn;

    @Column(nullable = false)
    String status;

    Instant createdAt;
}

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
@Table(
    name = "reservations",
    indexes = @Index(name = "uk_reservations_rrn", columnList = "rrn", unique = true)
)
@NoArgsConstructor
@AllArgsConstructor
public class ReservationEntity {

    @Id
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(length = 16, nullable = false)
    private String pan;

    long reservationAmount;

    @Column(length = 12, unique = true, nullable = false)
    String rrn;

    @Column(nullable = false)
    String status;

    Instant createdAt;

    Instant updatedAt;
}

package com.processing.cardmanagement.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
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

    private BigDecimal reservationAmount;

    @Column(length = 12, unique = true, nullable = false)
    private String rrn;

    @Column(nullable = false)
    private String status;

    private Instant createdAt;

    private Instant updatedAt;
}

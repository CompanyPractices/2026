package com.processing.cardmanagement.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_event")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OutboxEventEntity {

    @Id
    private UUID id;

    // Явно связываем с event_type из SQL
    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    // Явно связываем с created_at из SQL
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    // Явно связываем с processed_at из SQL
    @Column(name = "processed_at")
    private Instant processedAt;

    // Явно связываем с retry_count из SQL
    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    // Явно связываем с last_error из SQL
    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(nullable = false)
    private String status = EventStatus.PENDING.toString();
}

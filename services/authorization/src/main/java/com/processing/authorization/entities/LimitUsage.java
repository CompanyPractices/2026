package com.processing.authorization.entities;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;
import java.time.LocalDate;

@Entity
@Table(name = "limit_usage",
        uniqueConstraints = { @UniqueConstraint(columnNames = {"pan", "usage_date"}) },
        indexes = {
            @Index(name = "idx_limit_usage_card", columnList = "pan"),
            @Index(name = "idx_limit_usage_date", columnList = "usage_date")
        })
@Data
public class LimitUsage {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 16)
    private String pan;

    @Column(name = "usage_date", nullable = false)
    private Instant usageDate;

    @Column(name = "daily_amount", nullable = false)
    @ColumnDefault("0")
    private Long dailyAmount;

    @Column(name = "monthly_amount", nullable = false)
    @ColumnDefault("0")
    private Long monthlyAmount;

    @Column(name = "updated_at")
    @ColumnDefault("CURRENT_TIMESTAMP")
    private Instant updatedAt;
}

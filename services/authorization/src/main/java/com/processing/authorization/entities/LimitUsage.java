package com.processing.authorization.entities;

import com.processing.authorization.dto.CardResponse;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;
import java.util.UUID;
import java.time.LocalDate;

@Entity
@Table(name = "limit_usage",
        uniqueConstraints = { @UniqueConstraint(columnNames = {"card_id", "usage_date"}) },
        indexes = {
            @Index(name = "idx_limit_usage_card", columnList = "card_id"),
            @Index(name = "idx_limit_usage_date", columnList = "usage_date")
        })
@Data
public class LimitUsage {
    @Id
    @GeneratedValue(strategy=GenerationType.UUID)
    UUID id;

    @Column(name = "card_id", nullable = false)
    UUID cardId;

    @Column(name = "usage_date", nullable = false)
    LocalDate usageDate;

    @Column(name = "daily_amount", nullable = false)
    @ColumnDefault("0")
    Long dailyAmount;

    @Column(name = "monthly_amount", nullable = false)
    @ColumnDefault("0")
    Long monthlyAmount;

    @Column(name = "updated_at")
    LocalDateTime updatedAt;

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

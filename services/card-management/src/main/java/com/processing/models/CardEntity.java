package com.processing.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name = "cards", indexes = {
        @Index(name = "uk_cards_pan", columnList = "pan", unique = true),
        @Index(name = "idx_cards_issuer_id", columnList = "issuer_id"),
        @Index(name = "idx_cards_created_at", columnList = "created_at")
})
@NoArgsConstructor
public final class CardEntity {

    enum Status {
        ACTIVE,
        INACTIVE,
        BLOCKED,
        EXPIRED,
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(length = 16, unique = true, nullable = false)
    private String pan;

    @Column(length = 6, nullable = false)
    private String bin;

    @Column(nullable = false)
    private String cardholderName;

    @Setter(AccessLevel.NONE)
    @Column(name = "expiry_date", nullable = false, length = 4)
    private String strExpiryDate;

    @Transient
    private LocalDate expiryDate = null;

    @Column(length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status = Status.ACTIVE;

    @Column(length = 3, nullable = false)
    private String currencyCode = "643";

    @Min(0)
    private int dailyLimit = 15_000_000;

    @Min(0)
    private int monthlyLimit = 300_000_000;

    @Min(0)
    private int availableBalance = 1_000_000;

    @Column(length = 10, nullable = false)
    private String issuerId;

    private LocalDate createdAt = LocalDate.now();

    @Transient
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMyy");

    public CardEntity(
            String pan,
            String bin,
            String cardholderName,
            int dailyLimit,
            int monthlyLimit,
            int initialBalance
    ) {
        this.pan = pan;
        this.bin = bin;
        this.cardholderName = cardholderName;
        this.dailyLimit = dailyLimit;
        this.monthlyLimit = monthlyLimit;
        this.availableBalance = initialBalance;
        setExpiryDate(LocalDate.now().plusYears(3));
    }

    public LocalDate getExpiryDate() {
        if (expiryDate == null && strExpiryDate != null) {
            expiryDate = LocalDate.parse(strExpiryDate, formatter);
        }

        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
        this.strExpiryDate = expiryDate.format(formatter);
    }
}

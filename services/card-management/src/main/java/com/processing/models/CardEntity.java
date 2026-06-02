package com.processing.models;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Entity
@Data
@Table(name = "cards")
public final class CardEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(length = 16, nullable = false)
    private String pan;

    @Column(length = 6, nullable = false)
    private String bin;

    @Column(nullable = false)
    private String cardholderName;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @Column(name = "expiry_date", nullable = false, length = 4)
    private String strExpiryDate;

    @Transient
    private LocalDate expiryDate = null;

    @Column(length = 20, nullable = false)
    private String status = "ACTIVE";

    @Column(length = 3, nullable = false)
    private String currencyCode = "643";

    private int dailyLimit = 15_000_000;

    private int monthlyLimit = 300_000_000;

    private int availableBalance = 1_000_000;

    @Column(length = 10, nullable = false)
    private String issuerId;

    private LocalDate createdAt = LocalDate.now();

    @Transient
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("ddMM");

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

package com.processing.cardmanagement.models;

import com.processing.cardmanagement.exceptions.InsufficientFundsException;
import com.processing.common.dto.cardmanagement.CardStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name = "cards", indexes = {
    @Index(name = "uk_cards_pan", columnList = "pan", unique = true),
    @Index(name = "idx_cards_issuer_id_created_at", columnList = "issuer_id, created_at"),
    @Index(name = "idx_cards_created_at", columnList = "created_at")
})
@NoArgsConstructor
@SQLDelete(sql = "UPDATE users SET status = 'DELETED' WHERE id = ?")
@SQLRestriction("status <> 'DELETED'")
public class CardEntity {

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
    private CardStatus status = CardStatus.ACTIVE;

    @Column(length = 3, nullable = false)
    private String currencyCode = "643";

    private long dailyLimit = 15_000_000;

    private long monthlyLimit = 300_000_000;

    private long availableBalance = 1_000_000;

    @Column(length = 10, nullable = false)
    private String issuerId;

    private LocalDate createdAt = LocalDate.now();

    @Transient
    private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMyy");

    public CardEntity(
        String pan,
        String bin,
        String cardholderName,
        String currencyCode,
        CardStatus status,
        long dailyLimit,
        long monthlyLimit,
        long initialBalance,
        String issuerId
    ) {
        this.pan = pan;
        this.bin = bin;
        this.cardholderName = cardholderName;
        this.currencyCode = currencyCode;
        this.status = status;
        this.dailyLimit = dailyLimit;
        this.monthlyLimit = monthlyLimit;
        this.availableBalance = initialBalance;
        this.issuerId = issuerId;
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

    public void reserve(long amount) {
        if (this.availableBalance < amount) {
            throw new InsufficientFundsException();
        }
        this.availableBalance -= amount;
    }
}

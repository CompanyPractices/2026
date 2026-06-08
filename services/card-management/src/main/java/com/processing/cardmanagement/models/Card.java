package com.processing.cardmanagement.models;

import com.processing.cardmanagement.exceptions.InsufficientFundsException;
import com.processing.common.dto.cardmanagement.CardStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.UUID;

@Getter
@AllArgsConstructor
@RequiredArgsConstructor
public final class Card {

    private final UUID id;
    private final String pan;
    private final String bin;
    private final String cardholderName;
    private final YearMonth expiryDate;
    private CardStatus status = CardStatus.ACTIVE;
    private String currencyCode = "643";
    private long dailyLimit = 15_000_000;
    private long monthlyLimit = 300_000_000;
    private long availableBalance = 1_000_000;
    private final String issuerId;
    private final LocalDateTime createdAt = LocalDateTime.now();

    public Card(
        String pan,
        String bin,
        String cardholderName,
        String currencyCode,
        long dailyLimit,
        long monthlyLimit,
        long initialBalance,
        String issuerId
    ) {
        this.id = UUID.randomUUID();
        this.pan = pan;
        this.bin = bin;
        this.cardholderName = cardholderName;
        this.expiryDate = YearMonth.now().plusYears(3);
        this.currencyCode = currencyCode;
        this.dailyLimit = dailyLimit;
        this.monthlyLimit = monthlyLimit;
        this.availableBalance = initialBalance;
        this.issuerId = issuerId;
    }

    public void reserve(long amount) {
        if (this.availableBalance < amount) {
            throw new InsufficientFundsException();
        }
        this.availableBalance -= amount;
    }

    public void delete() {
        if (this.status == CardStatus.DELETED) {
            throw new IllegalStateException("Card already deleted");
        }

        this.status = CardStatus.DELETED;
    }

    public void updateData(
        CardStatus status,
        Long dailyLimit,
        Long monthlyLimit,
        Long availableBalance
    ) {
        if (dailyLimit < 0 || monthlyLimit < 0) {
            throw new IllegalArgumentException("Limit can not contain negative values");
        }
        if (dailyLimit > monthlyLimit) {
            throw new IllegalArgumentException("Daily limit can not be larger than monthly limit");
        }

        this.status = status;
        this.dailyLimit = dailyLimit;
        this.monthlyLimit = monthlyLimit;
        this.availableBalance = availableBalance;
    }

    public static Card fromDraft(String pan, CardDraft draft) {
        return new Card(
            pan,
            draft.bin(),
            draft.cardholderName(),
            draft.currencyCode(),
            draft.dailyLimit(),
            draft.monthlyLimit(),
            draft.initialBalance(),
            draft.issuerId()
        );
    }
}

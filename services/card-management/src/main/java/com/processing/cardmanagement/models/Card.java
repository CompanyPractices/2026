package com.processing.cardmanagement.models;

import com.processing.cardmanagement.exceptions.InsufficientFundsException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Objects;
import java.util.UUID;

/**
 * Модель банковской карты
 *
 * @param id               уникальный идентификатор
 * @param pan              уникальный номер карты
 * @param bin              BIN номер карты
 * @param cardholderName   имя держателя карты
 * @param expiryDate       срок хранения карты
 * @param status           статус карты
 * @param currencyCode     код валюты
 * @param dailyLimit       дневной лимит карты
 * @param monthlyLimit     месячный лимит карты
 * @param availableBalance доступный баланс
 * @param issuerId         номер банка эмитента
 * @param createdAt        дата создания
 */
public record Card(
    UUID id,
    String pan,
    String bin,
    String cardholderName,
    YearMonth expiryDate,
    CardStatus status,
    String currencyCode,
    BigDecimal dailyLimit,
    BigDecimal monthlyLimit,
    BigDecimal availableBalance,
    String issuerId,
    LocalDateTime createdAt
) {

    public Card(
        UUID id,
        String pan,
        String bin,
        String cardholderName,
        YearMonth expiryDate,
        CardStatus status,
        String currencyCode,
        BigDecimal dailyLimit,
        BigDecimal monthlyLimit,
        BigDecimal availableBalance,
        String issuerId
    ) {
        this(
            id,
            pan,
            bin,
            cardholderName,
            expiryDate,
            status,
            currencyCode,
            dailyLimit,
            monthlyLimit,
            availableBalance,
            issuerId,
            LocalDateTime.now()
        );
    }

    /**
     * Создает копию карты с зарезервированным количеством средств
     *
     * @param reservation информация о резервировании
     * @return карта с измененным балансом
     */
    public Card applyReservation(Reservation reservation) {
        if (!Objects.equals(this.pan, reservation.pan())) {
            throw new IllegalArgumentException("Reservation PAN number and card PAN number differ");
        }
        BigDecimal amount = reservation.reservationAmount();
        if (this.availableBalance.compareTo(amount) < 0) {
            throw new InsufficientFundsException();
        }

        return new Card(
            id,
            pan,
            bin,
            cardholderName,
            expiryDate,
            status,
            currencyCode,
            dailyLimit,
            monthlyLimit,
            availableBalance.subtract(amount),
            issuerId,
            createdAt
        );
    }

    /**
     * Ставит карте статус "Удалено"
     *
     * @return удаленная карта
     */
    public Card deleted() {
        if (this.status == CardStatus.DELETED) {
            throw new IllegalStateException("Card already deleted");
        }

        return new Card(
            id,
            pan,
            bin,
            cardholderName,
            expiryDate,
            CardStatus.DELETED,
            currencyCode,
            dailyLimit,
            monthlyLimit,
            availableBalance,
            issuerId,
            createdAt
        );
    }

    /**
     * Изменяет некоторые параметры у карты
     *
     * @param status           новый статус
     * @param dailyLimit       новый дневной лимит
     * @param monthlyLimit     новый месячный лимит
     * @param availableBalance новый баланс
     * @return измененная карта
     * @throws IllegalArgumentException отрицательный лимит или дневной лимит > месячного
     */
    public Card withData(
        CardStatus status,
        BigDecimal dailyLimit,
        BigDecimal monthlyLimit,
        BigDecimal availableBalance
    ) {
        if (dailyLimit.compareTo(BigDecimal.ZERO) < 0 || monthlyLimit.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Limit can not contain negative values");
        }
        if (monthlyLimit.compareTo(dailyLimit) < 0) {
            throw new IllegalArgumentException("Daily limit can not be larger than monthly limit");
        }

        return new Card(
            id,
            pan,
            bin,
            cardholderName,
            expiryDate,
            status,
            currencyCode,
            dailyLimit,
            monthlyLimit,
            availableBalance,
            issuerId,
            createdAt
        );
    }

    /**
     * Создает заполненную карту из черновика
     *
     * @param pan      номер карты
     * @param issuerId номер банка эмитента
     * @param cardYtl  срок действия карты (в годах)
     * @param draft    частично заполненная карта
     * @return созданная карта
     */
    public static Card fromDraft(
        String pan,
        String issuerId,
        int cardYtl,
        CardDraft draft
    ) {
        return new Card(
            UUID.randomUUID(),
            pan,
            draft.bin(),
            draft.cardholderName(),
            YearMonth.now().plusYears(cardYtl),
            draft.status(),
            draft.currencyCode(),
            draft.dailyLimit(),
            draft.monthlyLimit(),
            draft.initialBalance(),
            issuerId
        );
    }
}

package com.processing.cardmanagement.services;

import com.processing.cardmanagement.exceptions.CardNotFoundException;
import com.processing.cardmanagement.models.Card;
import com.processing.cardmanagement.models.CardDraft;
import com.processing.cardmanagement.models.CardStatus;
import jakarta.annotation.Nullable;

import java.time.Instant;
import java.util.List;


public interface CardService {

    Card createCard(
        String bin,
        String cardholderName,
        String currencyCode,
        long dailyLimit,
        long monthlyLimit,
        long initialBalance
    );

    List<Card> createCards(List<CardDraft> data);

    Card getCard(String pan);

    /**
     * Возвращает список карт с пагинацией и фильтрацией
     *
     * @param limit     количество карт на странице
     * @param offset    смещение
     * @param status    фильтр по статусу
     * @param bin       фильтр по bin
     * @param issuerId  фильтр по IssuerId
     * @param startDate начало диапазона дат
     * @param endDate   конец диапазона дат
     * @return список карт
     */
    List<Card> getCards(
        @Nullable Integer limit,
        @Nullable Integer offset,
        @Nullable CardStatus status,
        @Nullable String bin,
        @Nullable String issuerId,
        @Nullable Instant startDate,
        @Nullable Instant endDate
    );

    /**
     * Частично обновляет параметры карты
     *
     * @param pan              PAN карты
     * @param status           новый статус карты
     * @param dailyLimit       новый дневной лимит карты
     * @param monthlyLimit     новый месячный лимит карты
     * @param availableBalance новый баланс карты
     * @return измененная карта
     * @throws CardNotFoundException если карта не найдена
     */
    Card patchCard(
        String pan,
        @Nullable CardStatus status,
        @Nullable Long dailyLimit,
        @Nullable Long monthlyLimit,
        @Nullable Long availableBalance
    );

    /**
     * Удаляет карту
     *
     * @param pan PAN карты
     * @throws CardNotFoundException если карта не найдена
     */
    void deleteCard(String pan);

    /**
     * Возвращает общее количество карт в базе данных
     *
     * @return количество карт
     */
    long countAllCards();

    /**
     * Считает количество карт, удовлетворяющее фильтрам
     *
     * @param status    фильтр по статусу
     * @param bin       фильтр по bin
     * @param issuerId  фильтр по IssuerId
     * @param startDate начало диапазона дат
     * @param endDate   конец диапазона дат
     * @return количество карт
     */
    long countCardsFiltered(
        @Nullable CardStatus status,
        @Nullable String bin,
        @Nullable String issuerId,
        @Nullable Instant startDate,
        @Nullable Instant endDate
    );

    /**
     * Резервирует средства на карте, уменьшая доступный баланс
     *
     * @param pan    PAN карты
     * @param amount размер резервирования
     * @return измененная карта
     * @throws CardNotFoundException если карта не найдена
     */
    Card reserve(String pan, long amount);
}

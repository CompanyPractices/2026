package com.processing.cardmanagement.repositories;

import com.processing.cardmanagement.models.Card;
import com.processing.cardmanagement.models.CardStatus;
import jakarta.annotation.Nullable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.UnaryOperator;

public interface CardRepository {

    Optional<Card> findByPan(String pan);

    /**
     * Возвращает список карт с фильтрацией и пагинацией
     * Параметры фильтрации опциональны, если передается null - фильтр по этому полю не применяется
     *
     * @return список карт
     */
    List<Card> findCards(
        long limit,
        long offset,
        @Nullable CardStatus status,
        @Nullable String bin,
        @Nullable String issuerId,
        @Nullable LocalDateTime startDate,
        @Nullable LocalDateTime endDate
    );

    /**
     * Возвращает количество карт с применением фильтров
     *
     * @return количество карт
     */
    long countCardsFiltered(
        @Nullable CardStatus status,
        @Nullable String bin,
        @Nullable String issuerId,
        @Nullable LocalDateTime startDate,
        @Nullable LocalDateTime endDate
    );

    /**
     * @return количество хранящихся карт
     */
    long countAllCards();

    Card save(Card card);

    List<Card> saveAll(List<Card> cards);

    /**
     * Обновляет значение в БД, используя пессимистичную блокировку
     * Работает в рамках одной транзакции
     *
     * @param pan           номер карты
     * @param businessLogic логика изменения карты
     * @return измененная карта
     * @throws NoSuchElementException если не была найдена карта
     */
    Card updateWithPessimisticLock(String pan, UnaryOperator<Card> businessLogic);
}

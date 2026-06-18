package com.processing.cardmanagement.repositories;

import com.processing.cardmanagement.models.Card;
import com.processing.cardmanagement.models.CardStatus;
import jakarta.annotation.Nullable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface CardRepository {

    Optional<Card> findByPan(String pan);

    /**
     * Находит карту по номеру PAN и блокирует ее параллельное изменение
     *
     * @param pan номер карты
     * @return найденная карта
     */
    Optional<Card> findByPanForUpdate(String pan);

    /**
     * Возвращает список карт с фильтрацией и пагинацией
     * Параметры фильтрации опциональны, если передается null - фильтр по этому полю не применяется
     *
     * @return список карт
     */
    List<Card> findCards(
        int limit,
        long offset,
        @Nullable CardStatus status,
        @Nullable String bin,
        @Nullable String issuerId,
        @Nullable Instant startDate,
        @Nullable Instant endDate
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
        @Nullable Instant startDate,
        @Nullable Instant endDate
    );

    /**
     * @return количество хранящихся карт
     */
    long countAllCards();

    Card save(Card card);

    List<Card> saveAll(List<Card> cards);
}

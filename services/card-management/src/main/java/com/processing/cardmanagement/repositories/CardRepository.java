package com.processing.cardmanagement.repositories;

import com.processing.cardmanagement.models.Card;
import com.processing.common.dto.cardmanagement.CardStatus;
import jakarta.annotation.Nullable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CardRepository {

    Optional<Card> findByPan(String pan);

    /**
     * Возвращает список карт с фильтрацией и пагинацией
     * Параметры фильтрации опциональны, если передается null - фильтр по этому полю не применяется
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
     * @return количество карт
     */
    long countCards(
        @Nullable CardStatus status,
        @Nullable String bin,
        @Nullable String issuerId,
        @Nullable LocalDateTime startDate,
        @Nullable LocalDateTime endDate
    );

    /**
     * @return количество хранящихся карт
     */
    long countCards();

    Card save(Card card);

    List<Card> saveAll(List<Card> cards);
}

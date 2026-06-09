package com.processing.cardmanagement.repositories;

import com.processing.cardmanagement.models.Card;
import com.processing.common.dto.cardmanagement.CardStatus;
import jakarta.annotation.Nullable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с банковскими картами
 */
public interface CardRepository {

    /**
     * Находит карту по номеру PAN
     *
     * @param pan 16-значный PAN карты
     * @return карта
     */
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

    /**
     * Добавляет новую карту или изменяет существующую
     * @param card новая или обновленная карта
     * @return добавленные или измененные карты
     */
    Card save(Card card);

    /**
     * Сохраняет или изменяет все указанные карты
     * @param cards новые или обновленные карты
     * @return добавленные или измененные карты
     */
    List<Card> saveAll(List<Card> cards);
}

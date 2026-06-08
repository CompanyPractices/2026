package com.processing.cardmanagement.repositories;

import com.processing.cardmanagement.models.CardEntity;
import com.processing.common.dto.cardmanagement.CardStatus;
import jakarta.annotation.Nullable;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Репозиторий для работы с банковскими картами
 */
@Repository
public interface CardRepository
    extends JpaRepository<CardEntity, UUID> {

    /**
     * Находит карту по номеру PAN
     *
     * @param pan 16-значный PAN карты
     * @return карта
     */
    Optional<CardEntity> findByPan(String pan);

    /**
     * Возвращает список карт с фильтрацией и пагинацией
     * Параметры фильтрации опциональны, если передается null - фильтр по этому полю не применяется
     * @return список карт
     */
    @Query(value = """
        SELECT card
        FROM CardEntity card
        WHERE
            (:status IS NULL OR card.status = :status) AND
            (:bin IS NULL OR card.bin = :bin) AND
            (:issuerId IS NULL OR card.issuerId = :issuerId) AND
            (:startDate IS NULL OR card.createdAt >= :startDate) AND
            (:endDate IS NULL OR card.createdAt <= :endDate)
        """)
    List<CardEntity> findCards(
        @Nullable @Param("status") CardStatus status,
        @Nullable @Param("bin") String bin,
        @Nullable @Param("issuerId") String issuerId,
        @Nullable @Param("startDate") LocalDateTime startDate,
        @Nullable @Param("endDate") LocalDateTime endDate,
        Pageable pageable
    );

    /**
     * Возвращает количество карт с применением фильтров
     * @return количество карт
     */
    @Query(value = """
        SELECT COUNT(card)
        FROM CardEntity card
        WHERE
            (:status IS NULL OR card.status = :status) AND
            (:bin IS NULL OR card.bin = :bin) AND
            (:issuerId IS NULL OR card.issuerId = :issuerId) AND
            (:startDate IS NULL OR card.createdAt >= :startDate) AND
            (:endDate IS NULL OR card.createdAt <= :endDate)

        """)
    int countCards(
            @Nullable @Param("status") CardStatus status,
            @Nullable @Param("bin") String bin,
            @Nullable @Param("issuerId") String issuerId,
            @Nullable @Param("startDate") LocalDateTime startDate,
            @Nullable @Param("endDate") LocalDateTime endDate
    );
}

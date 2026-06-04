package com.processing.cardmanagement.repositories;

import com.processing.cardmanagement.models.CardEntity;
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

@Repository
public interface CardRepository
    extends JpaRepository<CardEntity, UUID> {

    Optional<CardEntity> findByPan(String pan);

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
        @Nullable @Param("status") CardEntity.Status status,
        @Nullable @Param("bin") String bin,
        @Nullable @Param("issuerId") String issuerId,
        @Nullable @Param("startDate") LocalDateTime startDate,
        @Nullable @Param("endDate") LocalDateTime endDate,
        Pageable pageable
    );
}

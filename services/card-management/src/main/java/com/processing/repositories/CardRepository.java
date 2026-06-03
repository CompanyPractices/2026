package com.processing.repositories;

import com.processing.models.CardEntity;
import jakarta.annotation.Nullable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
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
            (:pan IS NULL OR card.pan = :pan) AND
            (:issuerId IS NULL OR card.issuerId = :issuerId) AND
            (:startDate IS NULL OR card.createdAt >= :startDate) AND
            (:endDate IS NULL OR card.createdAt <= :endDate)
        """)
    List<CardEntity> findCards(
        @Nullable @Param("pan") String pan,
        @Nullable @Param("issuerId") String issuerId,
        @Nullable @Param("startDate") LocalDate startDate,
        @Nullable @Param("endDate") LocalDate endDate
    );
}

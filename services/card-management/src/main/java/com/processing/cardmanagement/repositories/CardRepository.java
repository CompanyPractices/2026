package com.processing.cardmanagement.repositories;

import com.processing.cardmanagement.models.Card;
import com.processing.common.dto.cardmanagement.CardStatus;
import jakarta.annotation.Nullable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CardRepository {

    Optional<Card> findByPan(String pan);

    List<Card> findCards(
        long limit,
        long offset,
        @Nullable CardStatus status,
        @Nullable String bin,
        @Nullable String issuerId,
        @Nullable LocalDateTime startDate,
        @Nullable LocalDateTime endDate
    );

    long countCards(
        @Nullable CardStatus status,
        @Nullable String bin,
        @Nullable String issuerId,
        @Nullable LocalDateTime startDate,
        @Nullable LocalDateTime endDate
    );

    long countCards();

    Card save(Card card);

    List<Card> saveAll(List<Card> card);
}

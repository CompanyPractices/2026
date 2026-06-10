package com.processing.cardmanagement.repositories;

import com.processing.cardmanagement.models.CardEntity;
import jakarta.annotation.Nullable;

import java.time.LocalDateTime;
import java.util.List;

public interface CardCriteriaBuilderJpaRepository {

    List<CardEntity> findCards(
        long limit,
        long offset,
        @Nullable String status,
        @Nullable String bin,
        @Nullable String issuerId,
        @Nullable LocalDateTime startDate,
        @Nullable LocalDateTime endDate
    );

    long countCards(
        @Nullable String status,
        @Nullable String bin,
        @Nullable String issuerId,
        @Nullable LocalDateTime startDate,
        @Nullable LocalDateTime endDate
    );
}

package com.processing.cardmanagement.services;

import com.processing.cardmanagement.models.Card;
import com.processing.cardmanagement.models.CardDraft;
import com.processing.common.dto.cardmanagement.CardStatus;
import jakarta.annotation.Nullable;

import java.time.LocalDateTime;
import java.util.List;

public interface CardUseCase {

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

    List<Card> getCards(
        @Nullable Integer limit,
        @Nullable Integer offset,
        @Nullable CardStatus status,
        @Nullable String bin,
        @Nullable String issuerId,
        @Nullable LocalDateTime startDate,
        @Nullable LocalDateTime endDate
    );

    Card patchCard(
        String pan,
        @Nullable CardStatus status,
        @Nullable Long dailyLimit,
        @Nullable Long monthlyLimit,
        @Nullable Long availableBalance
    );

    Card deleteCard(String pan);

    long countCards();

    long countCards(
        @Nullable CardStatus status,
        @Nullable String bin,
        @Nullable String issuerId,
        @Nullable LocalDateTime startDate,
        @Nullable LocalDateTime endDate
    );

    Card reserve(String pan, long amount);
}

package com.processing.cardmanagement.services;

import com.processing.cardmanagement.exceptions.CardNotFoundException;
import com.processing.cardmanagement.models.Card;
import com.processing.cardmanagement.models.CardDraft;
import com.processing.cardmanagement.models.CardStatus;
import com.processing.cardmanagement.options.CardServiceDefaults;
import com.processing.cardmanagement.options.CardServiceSettings;
import com.processing.cardmanagement.repositories.CardRepository;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
public class CardServiceImpl implements CardService {

    private final CardRepository cardRepository;
    private final CardServiceSettings settings;
    private final CardServiceDefaults defaults;
    private final PanGenerator panGenerator;

    public Card createCard(
        String bin,
        String cardholderName,
        String currencyCode,
        long dailyLimit,
        long monthlyLimit,
        long initialBalance
    ) {
        var draft = new CardDraft(
            bin,
            cardholderName,
            CardStatus.ACTIVE,
            currencyCode,
            dailyLimit,
            monthlyLimit,
            initialBalance
        );

        return cardRepository.save(Card.fromDraft(
            panGenerator.generatePan(draft.bin()),
            settings.issuerId(),
            settings.cardYtl(),
            draft
        ));
    }

    public List<Card> createCards(List<CardDraft> data) {
        var entities = data
            .stream()
            .map(draft -> Card.fromDraft(
                panGenerator.generatePan(draft.bin()),
                settings.issuerId(),
                settings.cardYtl(),
                draft
            ))
            .toList();

        return cardRepository.saveAll(entities);
    }

    public Card getCard(String pan) {
        return cardRepository
            .findByPan(pan)
            .orElseThrow(CardNotFoundException::new);
    }

    public List<Card> getCards(
        @Nullable Integer limit,
        @Nullable Integer offset,
        @Nullable CardStatus status,
        @Nullable String bin,
        @Nullable String issuerId,
        @Nullable LocalDateTime startDate,
        @Nullable LocalDateTime endDate
    ) {
        return cardRepository.findCards(
            limit != null ? limit : defaults.pageLimit(),
            offset != null ? offset : defaults.pageOffset(),
            status,
            bin,
            issuerId,
            startDate,
            endDate
        );
    }

    public Card patchCard(
        String pan,
        @Nullable CardStatus status,
        @Nullable Long dailyLimit,
        @Nullable Long monthlyLimit,
        @Nullable Long availableBalance
    ) {
        var card = getCard(pan);
        return cardRepository.save(
            card.withData(
                status != null ? status : card.status(),
                dailyLimit != null ? dailyLimit : card.dailyLimit(),
                monthlyLimit != null ? monthlyLimit : card.monthlyLimit(),
                availableBalance != null ? availableBalance : card.availableBalance()
            )
        );
    }

    public void deleteCard(String pan) {
        var card = getCard(pan);
        cardRepository.save(card.deleted());
    }

    public long countCards() {
        return cardRepository.countCards();
    }

    public long countCards(
        @Nullable CardStatus status,
        @Nullable String bin,
        @Nullable String issuerId,
        @Nullable LocalDateTime startDate,
        @Nullable LocalDateTime endDate
    ) {
        return cardRepository.countCards(
            status,
            bin,
            issuerId,
            startDate,
            endDate
        );
    }

    public Card reserve(String pan, long amount) {
        var card = getCard(pan);
        return cardRepository.save(card.withReserved(amount));
    }
}

package com.processing.cardmanagement.services;

import com.processing.cardmanagement.events.domain.*;
import com.processing.cardmanagement.exceptions.CardNotFoundException;
import com.processing.cardmanagement.models.Card;
import com.processing.cardmanagement.models.CardDraft;
import com.processing.cardmanagement.models.CardStatus;
import com.processing.cardmanagement.options.CardServiceDefaults;
import com.processing.cardmanagement.options.CardServiceSettings;
import com.processing.cardmanagement.repositories.CardRepository;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class CardServiceImpl implements CardService {

    private final CardRepository cardRepository;
    private final CardServiceSettings settings;
    private final CardServiceDefaults defaults;
    private final PanGenerator panGenerator;
    private final CardServiceEventListener eventListener;

    @Override
    public Card createCard(
        String bin,
        String cardholderName,
        String currencyCode,
        BigDecimal dailyLimit,
        BigDecimal monthlyLimit,
        BigDecimal initialBalance
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

        var savedCard = cardRepository.save(Card.fromDraft(
            panGenerator.generatePan(draft.bin()),
            settings.issuerId(),
            settings.cardYtl(),
            draft
        ));

        eventListener.onEvent(new CardServiceCreationEvent(1));
        return savedCard;
    }

    @Override
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

        var saved = cardRepository.saveAll(entities);
        eventListener.onEvent(new CardServiceCreationEvent(saved.size()));
        return saved;
    }

    @Override
    public Card getCard(String pan) {
        return cardRepository
            .findByPan(pan)
            .orElseThrow(() -> new CardNotFoundException(pan));
    }

    @Override
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

    @Override
    public Card patchCard(
        String pan,
        @Nullable CardStatus status,
        @Nullable BigDecimal dailyLimit,
        @Nullable BigDecimal monthlyLimit,
        @Nullable BigDecimal availableBalance
    ) {
        var card = getCard(pan);
        var saved = cardRepository.save(
            card.withData(
                status != null ? status : card.status(),
                dailyLimit != null ? dailyLimit : card.dailyLimit(),
                monthlyLimit != null ? monthlyLimit : card.monthlyLimit(),
                availableBalance != null ? availableBalance : card.availableBalance()
            )
        );

        eventListener.onEvent(new CardServicePatchEvent(maskPan(pan)));
        return saved;
    }

    @Override
    public void deleteCard(String pan) {
        cardRepository.save(getCard(pan).deleted());
        eventListener.onEvent(new CardServiceDeletionEvent(maskPan(pan)));
    }

    @Override
    public long countCardsFiltered() {
        return cardRepository.countCards();
    }

    @Override
    public long countCardsFiltered(
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

    @Override
    public Card reserve(String pan, BigDecimal amount) {
        var reserved = cardRepository.save(getCard(pan).withReserved(amount));
        eventListener.onEvent(new CardServiceReserveEvent(maskPan(pan), amount));
        return reserved;
    }

    private String maskPan(String pan) {
        return pan.substring(0, 6) + "******" + pan.substring(pan.length() - 4);
    }
}

package com.processing.cardmanagement.services;

import com.processing.cardmanagement.exceptions.CardNotFoundException;
import com.processing.cardmanagement.models.Card;
import com.processing.cardmanagement.models.CardDraft;
import com.processing.cardmanagement.options.CardServiceSettings;
import com.processing.cardmanagement.repositories.CardRepository;
import com.processing.common.dto.cardmanagement.CardStatus;
import com.processing.common.dto.cardmanagement.ReserveRequest;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CardService implements CardUseCase {

    private static final long DEFAULT_OFFSET = 0;
    private static final long DEFAULT_LIMIT = 50;

    private final CardRepository cardRepository;
    private final CardServiceSettings settings;
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
            initialBalance,
            settings.issuerId()
        );

        return cardRepository.save(Card.fromDraft(
            panGenerator.generatePan(draft.bin()), draft)
        );
    }

    public List<Card> createGeneratedCards(List<CardDraft> data) {
        var entities = data
            .stream()
            .map(draft -> Card.fromDraft(
                panGenerator.generatePan(draft.bin()), draft)
            )
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
            limit != null ? limit : DEFAULT_LIMIT,
            offset != null ? offset : DEFAULT_OFFSET,
            status,
            bin,
            issuerId,
            startDate,
            endDate
        );
    }

    public void patchCard(
        String pan,
        @Nullable CardStatus status,
        @Nullable Long dailyLimit,
        @Nullable Long monthlyLimit,
        @Nullable Long availableBalance
    ) {
        var card = getCard(pan);
        card.updateData(
            status != null ? status : card.getStatus(),
            dailyLimit != null ? dailyLimit : card.getDailyLimit(),
            monthlyLimit != null ? monthlyLimit : card.getMonthlyLimit(),
            availableBalance != null ? availableBalance : card.getAvailableBalance()
        );

        cardRepository.save(card);
    }

    public void deleteCard(String pan) {
        var card = getCard(pan);
        card.delete();
        cardRepository.save(card);
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

    public void reserve(String pan, ReserveRequest data) {
        var card = getCard(pan);
        card.reserve(data.amount());
        cardRepository.save(card);
    }
}

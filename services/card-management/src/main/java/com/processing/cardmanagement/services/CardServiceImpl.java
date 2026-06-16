package com.processing.cardmanagement.services;

import com.processing.cardmanagement.events.CardEventNotifier;
import com.processing.cardmanagement.events.CardServiceCreationEvent;
import com.processing.cardmanagement.events.CardServiceDeletionEvent;
import com.processing.cardmanagement.events.CardServicePatchEvent;
import com.processing.cardmanagement.exceptions.CardNotFoundException;
import com.processing.cardmanagement.exceptions.TooLargeLimitException;
import com.processing.cardmanagement.models.Card;
import com.processing.cardmanagement.models.CardDraft;
import com.processing.cardmanagement.models.CardStatus;
import com.processing.cardmanagement.models.Reservation;
import com.processing.cardmanagement.options.CardServiceDefaults;
import com.processing.cardmanagement.options.CardServiceSettings;
import com.processing.cardmanagement.repositories.CardRepository;
import com.processing.cardmanagement.repositories.ReservationRepository;
import com.processing.cardmanagement.repositories.ReservationRollbackRepository;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

@RequiredArgsConstructor
public class CardServiceImpl implements CardService {

    private final TransactionService transactionService;
    private final CardRepository cardRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationRollbackRepository reservationRollbackRepository;
    private final CardServiceSettings settings;
    private final CardServiceDefaults defaults;
    private final PanGenerator panGenerator;
    private final CardEventNotifier eventNotifier;

    @Override
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

        var savedCard = cardRepository.save(Card.fromDraft(
            panGenerator.generatePan(draft.bin()),
            settings.issuerId(),
            settings.cardValidityPeriod(),
            draft
        ));

        eventNotifier.onEvent(new CardServiceCreationEvent(1));
        return savedCard;
    }

    @Override
    public List<Card> createCards(List<CardDraft> data) {
        var entities = data
            .stream()
            .map(draft -> Card.fromDraft(
                panGenerator.generatePan(draft.bin()),
                settings.issuerId(),
                settings.cardValidityPeriod(),
                draft
            ))
            .toList();

        var saved = cardRepository.saveAll(entities);
        eventNotifier.onEvent(new CardServiceCreationEvent(saved.size()));
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
        @Nullable Long offset,
        @Nullable CardStatus status,
        @Nullable String bin,
        @Nullable String issuerId,
        @Nullable LocalDateTime startDate,
        @Nullable LocalDateTime endDate
    ) {
        if (limit != null && limit > settings.maxPageLimit()) {
            throw new TooLargeLimitException(limit, settings.maxPageLimit());
        }
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
        @Nullable Long dailyLimit,
        @Nullable Long monthlyLimit,
        @Nullable Long availableBalance
    ) {
        try {
            var updated = cardRepository.updateWithPessimisticLock(pan, card -> card.withData(
                status != null ? status : card.status(),
                dailyLimit != null ? dailyLimit : card.dailyLimit(),
                monthlyLimit != null ? monthlyLimit : card.monthlyLimit(),
                availableBalance != null ? availableBalance : card.availableBalance()
            ));
            eventNotifier.onEvent(new CardServicePatchEvent(maskPan(pan)));
            return updated;
        } catch (NoSuchElementException ex) {
            throw new CardNotFoundException(pan);
        }
    }

    @Override
    public void deleteCard(String pan) {
        cardRepository.save(getCard(pan).deleted());
        eventNotifier.onEvent(new CardServiceDeletionEvent(maskPan(pan)));
    }

    @Override
    public long countAllCards() {
        return cardRepository.countAllCards();
    }

    @Override
    public long countCardsFiltered(
        @Nullable CardStatus status,
        @Nullable String bin,
        @Nullable String issuerId,
        @Nullable LocalDateTime startDate,
        @Nullable LocalDateTime endDate
    ) {
        return cardRepository.countCardsFiltered(
            status,
            bin,
            issuerId,
            startDate,
            endDate
        );
    }

    @Override
    public Card reserve(String pan, long amount, String rrn) {
        try {
            return cardRepository.updateWithPessimisticLock(pan, card -> {
                var reservation = reservationRepository.saveTransactional(
                    new Reservation(pan, amount, rrn)
                );
                card.applyReservation(reservation);
                return card;
            });
        } catch (NoSuchElementException ex) {
            throw new CardNotFoundException(pan);
        }
    }

    @Override
    public Card rollback(String pan, long amount, String rrn) {
        return null;
    }

    private String maskPan(String pan) {
        return pan.substring(0, 6) + "******" + pan.substring(pan.length() - 4);
    }
}

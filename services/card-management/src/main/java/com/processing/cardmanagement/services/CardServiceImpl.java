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
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
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
        log.info(
            "Creating a new card. Holder: {}, BIN: {}, Currency: {}",
            cardholderName,
            bin,
            currencyCode
        );

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

        log.info(
            "Card successfully created. ID: {}, PAN: {}",
            savedCard.id(),
            maskPan(savedCard.pan())
        );

        return savedCard;
    }

    public List<Card> createCards(List<CardDraft> data) {
        log.info("Bulk creating cards. Count: {}", data.size());

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
        log.info("Bulk creation completed. Successfully saved {} cards", saved.size());
        return saved;
    }

    public Card getCard(String pan) {
        log.debug("Fetching card by PAN: {}", maskPan(pan));
        return cardRepository
            .findByPan(pan)
            .orElseThrow(() -> new CardNotFoundException(maskPan(pan)));
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
        log.debug(
            "Filtering cards. Status: {}, BIN: {}, Limit: {}",
            status,
            bin,
            limit
        );
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
        log.info(
            "Patching card PAN: {}. New status: {}, New daily limit: {}",
            maskPan(pan),
            status,
            dailyLimit
        );

        var card = getCard(pan);
        var saved = cardRepository.save(
            card.withData(
                status != null ? status : card.status(),
                dailyLimit != null ? dailyLimit : card.dailyLimit(),
                monthlyLimit != null ? monthlyLimit : card.monthlyLimit(),
                availableBalance != null ? availableBalance : card.availableBalance()
            )
        );
        log.info("Card PAN: {} successfully updated", maskPan(pan));
        return saved;
    }

    public void deleteCard(String pan) {
        log.info("Initiating deletion/block for card PAN: {}", maskPan(pan));

        cardRepository.save(getCard(pan).deleted());
        log.info("Card PAN: {} status changed to DELETED", maskPan(pan));
    }

    public long countCards() {
        log.debug("Initiating counting cards");
        var cardsAmount = cardRepository.countCards();
        log.debug("Found {} cards in repository", cardsAmount);
        return cardsAmount;
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
        log.info("Reserving amount: {} for card PAN: {}", amount, maskPan(pan));
        var reserved = cardRepository.save(getCard(pan).withReserved(amount));
        log.info(
            "Successfully reserved {} for card PAN: {}. New balance: {}",
            amount,
            maskPan(pan),
            reserved.availableBalance()
        );
        return reserved;
    }

    private String maskPan(String pan) {
        return pan.substring(0, 6) + "******" + pan.substring(pan.length() - 4);
    }
}

package com.processing.services;

import com.processing.exceptions.CardNotFoundException;
import com.processing.models.*;
import com.processing.options.CardServiceOptions;
import com.processing.repositories.CardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CardService {

    private final CardRepository cardRepository;
    private final CardServiceOptions options;
    private final PanGenerator panGenerator;

    public CardDto createCard(CreateCardRequest data) {
        log.warn(options.issuerId());
        var cardEntity = new CardEntity(
            panGenerator.generatePan(data.bin()),
            data.bin(),
            data.cardholderName(),
            data.currencyCode(),
            data.dailyLimit(),
            data.monthlyLimit(),
            data.initialBalance(),
            options.issuerId()
        );

        return CardDto.fromEntity(cardRepository.save(cardEntity));
    }

    public CardDto getCard(String pan) {
        return cardRepository
            .findByPan(pan)
            .map(CardDto::fromEntity)
            .orElseThrow(CardNotFoundException::new);
    }

    public List<CardDto> getCards(GetCardsRequest data) {
        return cardRepository
            .findCards(
                data.pan(),
                data.issuerId(),
                data.startDate(),
                data.endDate(),
                PageRequest.of(data.offset(), data.limit())
            )
            .stream()
            .map(CardDto::fromEntity)
            .toList();
    }

    public void patchCard(String pan, PatchCardRequest data) {
        var card = cardRepository
            .findByPan(pan)
            .orElseThrow(CardNotFoundException::new);

        if (data.status() != null) {
            card.setStatus(data.status());
        }
        if (data.dailyLimit() != null) {
            card.setDailyLimit(data.dailyLimit());
        }
        if (data.monthlyLimit() != null) {
            card.setMonthlyLimit(data.monthlyLimit());
        }
        if (data.availableBalance() != null) {
            card.setAvailableBalance(data.availableBalance());
        }

        cardRepository.save(card);
    }

    public void deleteCard(String pan) {
        var card = cardRepository
            .findByPan(pan)
            .orElseThrow(CardNotFoundException::new);

        card.delete();
        cardRepository.save(card);
    }

    public long countCards() {
        return cardRepository.count();
    }
}

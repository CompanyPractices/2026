package com.processing.services;

import com.processing.exceptions.CardNotFoundException;
import com.processing.models.*;
import com.processing.options.CardServiceOptions;
import com.processing.repositories.CardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CardService {

    private final CardRepository cardRepository;
    private final CardServiceOptions options;
    private final PanGenerator panGenerator;

    public CardDto createCard(CreateCardRequest data) {
        var cardEntity = new CardEntity(
            data.bin(),
            data.cardholderName(),
            data.concurrencyCode(),
            data.dailyLimit(),
            data.monthlyLimit(),
            data.initialBalance(),
            options.issuerId()
        );

        cardEntity.generatePan(data.bin(), panGenerator);
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
                data.endDate()
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
}

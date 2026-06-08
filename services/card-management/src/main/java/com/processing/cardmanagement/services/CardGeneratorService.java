package com.processing.cardmanagement.services;

import com.processing.cardmanagement.options.CardGeneratorOptions;
import com.processing.common.dto.cardmanagement.CardStatus;
import com.processing.common.dto.cardmanagement.GeneratedCardDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.processing.common.dto.cardmanagement.CardModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class CardGeneratorService {

    private final CardService cardService;
    private final CardGeneratorOptions generatorOptions;

    private final Random random = new Random();

    private static final List<String> NAMES = List.of(
        "IVAN IVANOV", "PETR PETROV", "ANNA SMIRNOVA", "ELENA VOLKOVA", "DMITRY SOKOLOV"
    );

    public List<CardModel> generate(int count, List<String> bins) {
        List<GeneratedCardDto> cards = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            String bin = bins.get(i % bins.size());

            String cardholderName = NAMES.get(random.nextInt(NAMES.size()));
            int balance = random.nextInt(generatorOptions.minBalance(), generatorOptions.maxBalance());
            int dailyLimit = random.nextInt(generatorOptions.minDailyLimit(), generatorOptions.maxDailyLimit());
            int monthlyLimit = dailyLimit * 30;

            GeneratedCardDto card = new GeneratedCardDto(
                bin,
                cardholderName,
                generatorOptions.currencyCode(),
                dailyLimit,
                monthlyLimit,
                balance,
                generateStatus()
            );

            cards.add(card);
        }
        return cardService.createCards(cards);
    }

    private CardStatus generateStatus() {
        int roll = random.nextInt(100);

        if (roll < 95) {
            return CardStatus.ACTIVE;
        }

        if (roll < 98) {
            return CardStatus.INACTIVE;
        }

        return CardStatus.BLOCKED;
    }
}

package com.processing.services;

import com.processing.models.CardEntity;
import com.processing.repositories.CardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class CardGeneratorService {
    private final CardRepository cardRepository;

    private static final Random random = new Random();
    private static final String ISSUER_ID = "BANK";

    private static final List<String> NAMES = List.of(
          "IVAN IVANOV", "PETR PETROV", "ANNA SMIRNOVA", "ELENA VOLKOVA", "DMITRY SOKOLOV"
    );

    public List<CardEntity> generate(int count, List<String> bins) {
        List<CardEntity> cards = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            String bin = bins.get(i % bins.size());

            int balance = random.nextInt(1_000_000, 50_000_000);
            int dailyLimit = random.nextInt(5_000_000, 30_000_000);
            int monthlyLimit = dailyLimit * 30;
            String cardholderName = NAMES.get(random.nextInt(NAMES.size()));

            CardEntity card = new CardEntity(
                    LuhnValidator.generatePan(bin),
                    bin,
                    cardholderName,
                    dailyLimit,
                    monthlyLimit,
                    balance
            );

            card.setIssuerId(ISSUER_ID);
            card.setStatus(generateStatus());

            cards.add(card);
        }
        return cardRepository.saveAll(cards);
    }

    private CardEntity.Status generateStatus() {
        int roll = random.nextInt(100);

        if (roll < 95) {
            return CardEntity.Status.ACTIVE;
        }

        if (roll < 98) {
            return CardEntity.Status.INACTIVE;
        }

        return CardEntity.Status.BLOCKED;
    }
}

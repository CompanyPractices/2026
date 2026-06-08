package com.processing.cardmanagement.services;

import com.processing.cardmanagement.models.CardEntity;
import com.processing.cardmanagement.options.CardServiceOptions;
import com.processing.cardmanagement.repositories.CardRepository;
import com.processing.common.dto.cardmanagement.CreateCardRequest;
import net.datafaker.Faker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Locale;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public final class CardServiceTest {

    private final String testCardNumber = "1234 5678 9101 1123".replace(" ", "");

    private final Faker faker = new Faker(Locale.ENGLISH);

    private final CardServiceOptions options = new CardServiceOptions("TESTISSUER");

    private final PanGenerator panGenerator = new PanGenerator() {
        @Override
        public boolean isValid(String pan) {
            return true;
        }

        @Override
        public String generatePan(String bin) {
            return testCardNumber;
        }
    };

    @Mock
    private CardRepository cardRepository;

    @InjectMocks
    private CardService cardService;

    @BeforeEach
    void setUp() {
        cardService = new CardService(
            cardRepository,
            options,
            panGenerator
        );
    }

    @Test
    void testCreateCard() {
        when(cardRepository.save(any(CardEntity.class))).thenAnswer(invocation -> {
            var data = (CardEntity) invocation.getArgument(0);
            data.setId(UUID.randomUUID());
            return data;
        });

        var request = new CreateCardRequest(
            faker.number().digits(6),
            faker.name().fullName().toUpperCase(Locale.ROOT),
            faker.number().digits(3),
            faker.number().numberBetween(0, 10000000),
            faker.number().numberBetween(0, 30000000),
            faker.number().numberBetween(0, 10000000)
        );
        cardService.createCard(request);
        verify(cardRepository, times(1)).save(any(CardEntity.class));
    }
}

package com.processing.cardmanagement.services;

import com.processing.cardmanagement.exceptions.CardNotFoundException;
import com.processing.cardmanagement.models.CardEntity;
import com.processing.cardmanagement.options.CardServiceOptions;
import com.processing.cardmanagement.repositories.CardRepository;
import com.processing.common.dto.cardmanagement.CardStatus;
import com.processing.common.dto.cardmanagement.CreateCardRequest;
import net.datafaker.Faker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    private final ArgumentCaptor<CardEntity> cardCaptor =
        ArgumentCaptor.forClass(CardEntity.class);

    @Mock
    private CardRepository cardRepository;

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
        verify(cardRepository, times(1)).save(cardCaptor.capture());
        var entity = cardCaptor.getValue();

        assertEquals(testCardNumber, entity.getPan());
        assertEquals(request.bin(), entity.getBin());
        assertEquals(request.cardholderName(), entity.getCardholderName());
        assertEquals(LocalDate.now().plusYears(3), entity.getExpiryDate());
        assertEquals(CardStatus.ACTIVE, entity.getStatus());
        assertEquals(request.currencyCode(), entity.getCurrencyCode());
        assertEquals(request.dailyLimit(), entity.getDailyLimit());
        assertEquals(request.monthlyLimit(), entity.getMonthlyLimit());
        assertEquals(request.initialBalance(), entity.getAvailableBalance());
    }

    @Test
    void testGetCard() {
        var pan = testCardNumber;

        var entity = createTestCardEntity(pan);
        when(cardRepository.findByPan(anyString())).thenReturn(Optional.of(entity));

        var found = cardService.getCard(pan);
        verify(cardRepository, times(1)).findByPan(anyString());

        assertEquals(entity.getId(), found.id());
        assertEquals(entity.getPan(), found.pan());
        assertEquals(entity.getBin(), found.bin());
        assertEquals(entity.getCardholderName(), found.cardholderName());
        assertEquals(entity.getStrExpiryDate(), found.expiryDate());
        assertEquals(entity.getStatus().name(), found.status());
        assertEquals(entity.getCurrencyCode(), found.currencyCode());
        assertEquals(entity.getDailyLimit(), found.dailyLimit());

        when(cardRepository.findByPan(anyString())).thenReturn(Optional.empty());
        assertThrows(CardNotFoundException.class, () -> cardService.getCard(testCardNumber));
    }

    private CardEntity createTestCardEntity(String pan) {
        var entity = new CardEntity();
        entity.setId(UUID.randomUUID());
        entity.setPan(pan);
        entity.setBin(pan.substring(0, 6));
        entity.setCardholderName(faker.name().fullName().toUpperCase(Locale.ROOT));
        entity.setExpiryDate(LocalDate.now().plusYears(3));
        entity.setIssuerId(options.issuerId());
        entity.setCreatedAt(LocalDateTime.now());
        return entity;
    }
}

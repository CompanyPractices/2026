package com.processing.cardmanagement.services;

import com.processing.cardmanagement.exceptions.CardNotFoundException;
import com.processing.cardmanagement.exceptions.InsufficientFundsException;
import com.processing.cardmanagement.models.CardEntity;
import com.processing.cardmanagement.options.CardServiceConfigurationProperties;
import com.processing.cardmanagement.repositories.CardJpaRepository;
import com.processing.common.dto.cardmanagement.*;
import net.datafaker.Faker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public final class CardServiceTest {

    private final String panGeneratorCardNumber = "1234 5678 9101 1123".replace(" ", "");

    private final Faker faker = new Faker(Locale.ENGLISH);

    private final CardServiceConfigurationProperties options = new CardServiceConfigurationProperties("TESTISSUER");

    private final PanGenerator panGenerator = new PanGenerator() {
        @Override
        public boolean isValid(String pan) {
            return true;
        }

        @Override
        public String generatePan(String bin) {
            return panGeneratorCardNumber;
        }
    };

    private final ArgumentCaptor<CardEntity> cardCaptor =
        ArgumentCaptor.forClass(CardEntity.class);

    @Mock
    private CardJpaRepository cardRepository;

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
            faker.number().numberBetween(0L, 10000000L),
            faker.number().numberBetween(0L, 30000000L),
            faker.number().numberBetween(0L, 10000000L)
        );

        var model = cardService.createCard(request);
        verify(cardRepository, times(1)).save(cardCaptor.capture());
        var entity = cardCaptor.getValue();

        assertEquals(panGeneratorCardNumber, entity.getPan());
        assertEquals(request.bin(), entity.getBin());
        assertEquals(request.cardholderName(), entity.getCardholderName());
        assertEquals(LocalDate.now().plusYears(3), entity.getExpiryDate());
        assertEquals(CardStatus.ACTIVE, entity.getStatus());
        assertEquals(request.currencyCode(), entity.getCurrencyCode());
        assertEquals(request.dailyLimit(), entity.getDailyLimit());
        assertEquals(request.monthlyLimit(), entity.getMonthlyLimit());
        assertEquals(request.initialBalance(), entity.getAvailableBalance());

        entity.setId(model.id());
        validateCardModel(entity, model);
    }

    @Test
    void testGetCard() {
        var pan = generatePan();

        var entity = createTestCardEntity(pan);
        when(cardRepository.findByPan(pan)).thenReturn(Optional.of(entity));

        var found = cardService.getCard(pan);
        validateCardModel(entity, found);

        when(cardRepository.findByPan(anyString())).thenReturn(Optional.empty());
        assertThrows(CardNotFoundException.class, () -> cardService.getCard(pan));
    }

    @Test
    void testGetCards() {
        var entity = createTestCardEntity(generatePan());

        int limit = faker.number().numberBetween(1, 100);
        int offset = faker.number().numberBetween(0, 100);
        CardStatus status = CardStatus.ACTIVE;
        String bin = faker.number().digits(6);
        String issuerId = faker.regexify("[A-Z0-9]{1,10}");
        LocalDateTime startDate = LocalDateTime.now().minusDays(1);
        LocalDateTime endDate = LocalDateTime.now();

        when(cardRepository.findCards(
            status,
            bin,
            issuerId,
            startDate,
            endDate,
            PageRequest.of(offset, limit)
        )).thenReturn(List.of(entity));

        when(cardRepository.countCards(
            status,
            bin,
            issuerId,
            startDate,
            endDate
        )).thenReturn(1);

        var result = cardService.getCards(limit, offset, status, bin, issuerId, startDate, endDate);
        validateCardModel(entity, result.cards().getFirst());
        assertEquals(1, result.total());
    }

    @Test
    void testPatchCard() {
        var pan = generatePan();
        var request = new PatchCardRequest(
            CardStatus.EXPIRED,
            faker.number().numberBetween(0L, 10000000L),
            faker.number().numberBetween(0L, 30000000L),
            faker.number().numberBetween(0L, 10000000L)
        );
        var entity = createTestCardEntity(pan);
        when(cardRepository.findByPan(pan)).thenReturn(Optional.of(entity));
        cardService.patchCard(pan, request);

        assertEquals(request.status(), entity.getStatus());
        assertEquals(request.dailyLimit(), entity.getDailyLimit());
        assertEquals(request.monthlyLimit(), entity.getMonthlyLimit());
        assertEquals(request.availableBalance(), entity.getAvailableBalance());
    }

    @Test
    void testDeleteCard() {
        var pan = generatePan();
        var entity = createTestCardEntity(pan);
        when(cardRepository.findByPan(pan)).thenReturn(Optional.of(entity));
        cardService.deleteCard(pan);
        verify(cardRepository, times(1)).delete(entity);
    }

    @Test
    void testCountCards() {
        var returnValue = 1L;
        when(cardRepository.count()).thenReturn(returnValue);
        assertEquals(returnValue, cardService.countCards());
    }

    @Test
    void testReserve() {
        var pan = generatePan();
        var entity = createTestCardEntity(pan);
        var data = new ReserveRequest(
            faker.number().numberBetween(0L, entity.getAvailableBalance()),
            faker.lorem().characters()
        );
        when(cardRepository.findByPan(pan)).thenReturn(Optional.of(entity));
        var prevBalance = entity.getAvailableBalance();
        cardService.reserve(pan, data);
        assertEquals(prevBalance - data.amount(), entity.getAvailableBalance());
        assertThrows(InsufficientFundsException.class, () ->
            cardService.reserve(pan, new ReserveRequest(
                Long.MAX_VALUE,
                faker.lorem().characters()
            ))
        );
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

    private void validateCardModel(CardEntity entity, CardModel model) {
        assertEquals(entity.getId(), model.id());
        assertEquals(entity.getPan(), model.pan());
        assertEquals(entity.getBin(), model.bin());
        assertEquals(entity.getCardholderName(), model.cardholderName());
        assertEquals(entity.getStrExpiryDate(), model.expiryDate());
        assertEquals(entity.getStatus().name(), model.status());
        assertEquals(entity.getCurrencyCode(), model.currencyCode());
        assertEquals(entity.getDailyLimit(), model.dailyLimit());
    }

    private String generatePan() {
        return faker.regexify("[0-9]{16}");
    }
}

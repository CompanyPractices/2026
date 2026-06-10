package com.processing.cardmanagement.services;

import com.processing.cardmanagement.exceptions.CardNotFoundException;
import com.processing.cardmanagement.exceptions.InsufficientFundsException;
import com.processing.cardmanagement.models.Card;
import com.processing.cardmanagement.options.CardServiceDefaults;
import com.processing.cardmanagement.options.CardServiceSettings;
import com.processing.cardmanagement.repositories.CardRepository;
import com.processing.common.dto.cardmanagement.CardStatus;
import net.datafaker.Faker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.YearMonth;
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

    private final CardServiceSettings settings = new CardServiceSettings(
        "TESTISSUER",
        3
    );

    private final CardServiceDefaults defaults = new CardServiceDefaults(
        0,
        50,
        "643",
        15000000,
        300000000,
        1000000
    );

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

    private final ArgumentCaptor<Card> cardCaptor =
        ArgumentCaptor.forClass(Card.class);

    @Mock
    private CardRepository cardRepository;

    private CardService cardService;

    @BeforeEach
    void setUp() {
        cardService = new CardServiceImpl(
            cardRepository,
            settings,
            defaults,
            panGenerator
        );
    }

    @Test
    void testCreateCard() {
        var bin = faker.number().digits(6);
        var cardholderName = faker.name().fullName().toUpperCase(Locale.ROOT);
        var currencyCode = faker.number().digits(3);
        var dailyLimit = faker.number().numberBetween(0L, 10000000L);
        var monthlyLimit = faker.number().numberBetween(dailyLimit, 30000000L);
        var initialBalance = faker.number().numberBetween(0L, 10000000L);
        var expDate = YearMonth.now().plusYears(settings.cardYtl());

        when(cardRepository.save(any(Card.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        var response = cardService.createCard(
            bin,
            cardholderName,
            currencyCode,
            dailyLimit,
            monthlyLimit,
            initialBalance
        );

        var expected = new Card(
            response.id(),
            panGeneratorCardNumber,
            bin,
            cardholderName,
            expDate,
            CardStatus.ACTIVE,
            currencyCode,
            dailyLimit,
            monthlyLimit,
            initialBalance,
            settings.issuerId(),
            response.createdAt()
        );

        assertEquals(expected, response);
    }

    @Test
    void testGetCard() {
        var pan = generatePan();

        var card = createTestCard(pan);
        when(cardRepository.findByPan(pan)).thenReturn(Optional.of(card));

        var found = cardService.getCard(pan);
        assertEquals(card, found);

        when(cardRepository.findByPan(anyString())).thenReturn(Optional.empty());
        assertThrows(CardNotFoundException.class, () -> cardService.getCard(pan));
    }

    @Test
    void testGetCards() {
        var testCard = createTestCard(generatePan());

        int limit = faker.number().numberBetween(1, 100);
        int offset = faker.number().numberBetween(0, 100);
        CardStatus status = CardStatus.ACTIVE;
        String bin = faker.number().digits(6);
        String issuerId = faker.regexify("[A-Z0-9]{1,10}");
        LocalDateTime startDate = LocalDateTime.now().minusDays(1);
        LocalDateTime endDate = LocalDateTime.now();

        when(cardRepository.findCards(
            limit,
            offset,
            status,
            bin,
            issuerId,
            startDate,
            endDate
        )).thenReturn(List.of(testCard));

        var cards = cardService.getCards(limit, offset, status, bin, issuerId, startDate, endDate);
        assertEquals(1, cards.size());
        assertEquals(testCard, cards.getFirst());
    }

    @Test
    void testPatchCard() {
        var pan = generatePan();
        var status = CardStatus.EXPIRED;
        var dailyLimit = faker.number().numberBetween(0L, 10000000L);
        var monthlyLimit = faker.number().numberBetween(dailyLimit, 30000000L);
        var availableBalance = faker.number().numberBetween(0L, 10000000L);
        var testCard = createTestCard(pan);

        when(cardRepository.findByPan(pan)).thenReturn(Optional.of(testCard));
        when(cardRepository.save(any(Card.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        var expected = new Card(
            testCard.id(),
            testCard.pan(),
            testCard.bin(),
            testCard.cardholderName(),
            testCard.expiryDate(),
            status,
            testCard.currencyCode(),
            dailyLimit,
            monthlyLimit,
            availableBalance,
            testCard.issuerId(),
            testCard.createdAt()
        );

        var card = cardService.patchCard(
            pan,
            status,
            dailyLimit,
            monthlyLimit,
            availableBalance
        );
        assertEquals(expected, card);
    }

    @Test
    void testDeleteCard() {
        var pan = generatePan();
        var testCard = createTestCard(pan);
        when(cardRepository.findByPan(pan)).thenReturn(Optional.of(testCard));
        when(cardRepository.save(any(Card.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        var expected = new Card(
            testCard.id(),
            testCard.pan(),
            testCard.bin(),
            testCard.cardholderName(),
            testCard.expiryDate(),
            CardStatus.DELETED,
            testCard.currencyCode(),
            testCard.dailyLimit(),
            testCard.monthlyLimit(),
            testCard.availableBalance(),
            testCard.issuerId(),
            testCard.createdAt()
        );

        cardService.deleteCard(pan);
        verify(cardRepository, times(1)).save(cardCaptor.capture());
        var deletedCard = cardCaptor.getValue();
        assertEquals(expected, deletedCard);
    }

    @Test
    void testCountCardsFiltered() {
        var amount = 1L;
        CardStatus status = CardStatus.ACTIVE;
        String bin = faker.number().digits(6);
        String issuerId = faker.regexify("[A-Z0-9]{1,10}");
        LocalDateTime startDate = LocalDateTime.now().minusDays(1);
        LocalDateTime endDate = LocalDateTime.now();

        when(cardRepository.countCards(
            status,
            bin,
            issuerId,
            startDate,
            endDate
        )).thenReturn(amount);

        var count = cardService.countCards(
            status,
            bin,
            issuerId,
            startDate,
            endDate
        );
        assertEquals(amount, count);
    }

    @Test
    void testCountCards() {
        var returnValue = 1L;
        when(cardRepository.countCards()).thenReturn(returnValue);
        assertEquals(returnValue, cardService.countCards());
    }

    @Test
    void testReserve() {
        var pan = generatePan();
        var testCard = createTestCard(pan);
        var reserveAmount = faker.number().numberBetween(0L, testCard.availableBalance());
        var prevBalance = testCard.availableBalance();
        when(cardRepository.findByPan(pan)).thenReturn(Optional.of(testCard));
        when(cardRepository.save(any(Card.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        var expected = new Card(
            testCard.id(),
            testCard.pan(),
            testCard.bin(),
            testCard.cardholderName(),
            testCard.expiryDate(),
            testCard.status(),
            testCard.currencyCode(),
            testCard.dailyLimit(),
            testCard.monthlyLimit(),
            testCard.availableBalance() - reserveAmount,
            testCard.issuerId(),
            testCard.createdAt()
        );

        var card = cardService.reserve(pan, reserveAmount);
        assertEquals(expected, card);
        assertThrows(InsufficientFundsException.class, () ->
            cardService.reserve(
                pan,
                Long.MAX_VALUE
            )
        );
    }

    private Card createTestCard(String pan) {
        return new Card(
            UUID.randomUUID(),
            pan,
            pan.substring(0, 6),
            faker.name().fullName().toUpperCase(Locale.ROOT),
            YearMonth.now().plusYears(settings.cardYtl()),
            CardStatus.ACTIVE,
            defaults.currencyCode(),
            defaults.dailyLimit(),
            defaults.monthlyLimit(),
            defaults.balance(),
            settings.issuerId()
        );
    }

    private String generatePan() {
        return faker.regexify("[0-9]{16}");
    }
}

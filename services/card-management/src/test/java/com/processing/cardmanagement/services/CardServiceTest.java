package com.processing.cardmanagement.services;

import com.processing.cardmanagement.events.CardEventNotifier;
import com.processing.cardmanagement.exceptions.*;
import com.processing.cardmanagement.models.*;
import com.processing.cardmanagement.options.CardServiceDefaults;
import com.processing.cardmanagement.options.CardServiceDefaultsConfigurationProperties;
import com.processing.cardmanagement.options.CardServiceSettings;
import com.processing.cardmanagement.options.CardServiceSettingsConfigurationProperties;
import com.processing.cardmanagement.repositories.CardRepository;
import com.processing.cardmanagement.repositories.ReservationRepository;
import com.processing.cardmanagement.repositories.ReservationRollbackRepository;
import com.processing.cardmanagement.services.retries.RetryServiceImpl;
import com.processing.cardmanagement.utils.CardManagementTestUtils;
import net.datafaker.Faker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static com.processing.cardmanagement.utils.CardManagementTestUtils.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CardServiceTest {

    private final String panGeneratorCardNumber = "1234 5678 9101 1123".replace(" ", "");

    private final Faker faker = new Faker(Locale.ENGLISH);

    private final String testIssuerId = "TEST_ISSUER";

    private final CardServiceSettings settings = new CardServiceSettingsConfigurationProperties(
        3,
        10000,
        3
    );

    private final CardServiceDefaults defaults = new CardServiceDefaultsConfigurationProperties(
        1,
        50,
        "643",
        BigDecimal.valueOf(15000000),
        BigDecimal.valueOf(300000000),
        BigDecimal.valueOf(300000000)
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

    private final ArgumentCaptor<Reservation> reservationCaptor =
        ArgumentCaptor.forClass(Reservation.class);

    private final ArgumentCaptor<ReservationRollback> reservationRollbackCaptor =
        ArgumentCaptor.forClass(ReservationRollback.class);

    @Mock
    private CardRepository cardRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private ReservationRollbackRepository reservationRollbackRepository;

    @Mock
    private CardEventNotifier eventNotifier;

    @Mock
    private BinIssuerService binIssuerService;

    private CardService cardService;

    @BeforeEach
    void setUp() {
        cardService = new CardServiceImpl(
            cardRepository,
            reservationRepository,
            reservationRollbackRepository,
            settings,
            defaults,
            panGenerator,
            eventNotifier,
            binIssuerService,
            new RetryServiceImpl(),
            new TransactionRunnerImpl()
        );
    }

    @Test
    void testCreateCardSuccess() {
        var bin = faker.number().digits(6);
        var cardholderName = faker.name().fullName().toUpperCase(Locale.ROOT);
        var currencyCode = faker.number().digits(3);
        var dailyLimit = BigDecimal.valueOf(faker.number().numberBetween(0, 10000000));
        var monthlyLimit = BigDecimal.valueOf(faker.number().numberBetween(dailyLimit.intValue(), 30000000));
        var initialBalance = BigDecimal.valueOf(faker.number().numberBetween(0, 10000000));
        var expDate = YearMonth.now().plusYears(settings.cardValidityPeriod());

        when(cardRepository.create(any(Card.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        when(binIssuerService.getIssuerId(bin)).thenReturn(testIssuerId);

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
            testIssuerId,
            response.createdAt()
        );

        assertEquals(expected, response);
    }

    @Test
    void createCardRetryThrowTest() {
        when(cardRepository.create(any(Card.class))).thenThrow(PanCollisionException.class);
        assertThrows(
            OutOfRetriesException.class,
            () -> cardService.createCard(
                "400000",
                "ANY_USER",
                "643",
                BigDecimal.TEN,
                BigDecimal.TEN,
                BigDecimal.ZERO
            )
        );
        verify(cardRepository, times(settings.maxCardCreationRetries() + 1)).create(any(Card.class));
    }

    @Test
    void createCardRetrySuccessTest() {
        var counter = new AtomicInteger(0);
        when(cardRepository.create(any(Card.class))).thenAnswer(invocation -> {
            counter.incrementAndGet();
            if (counter.get() <= settings.maxCardCreationRetries()) {
                throw new PanCollisionException("ANY_PAN");
            }
            return null;
        });
        assertDoesNotThrow(
            () -> cardService.createCard(
                "400000",
                "ANY_USER",
                "643",
                BigDecimal.TEN,
                BigDecimal.TEN,
                BigDecimal.ZERO
            )
        );
        verify(cardRepository, times(settings.maxCardCreationRetries() + 1)).create(any(Card.class));
    }

    @Test
    void createCardsRetrySuccessTest() {
        var cardsAmount = 10;
        when(cardRepository.createAll(any()))
            .thenThrow(MassiveCardCreationCollisionException.class);
        when(cardRepository.create(any()))
            .thenAnswer(invocation -> invocation.getArgument(0));
        var drafts = Stream
            .generate(CardManagementTestUtils::generateCardDraft)
            .limit(cardsAmount)
            .toList();
        cardService.createCards(drafts);
        verify(cardRepository, times(1)).createAll(any());
        verify(cardRepository, times(cardsAmount)).create(any());
    }

    @Test
    void createCardsRetryThrowTest() {
        var cardsAmount = 10;
        when(cardRepository.createAll(any()))
            .thenThrow(MassiveCardCreationCollisionException.class);
        when(cardRepository.create(any())).thenThrow(PanCollisionException.class);
        var drafts = Stream
            .generate(CardManagementTestUtils::generateCardDraft)
            .limit(cardsAmount)
            .toList();
        assertThrows(OutOfRetriesException.class, () -> {
            cardService.createCards(drafts);
        });
        verify(cardRepository, times(1)).createAll(any());
        verify(cardRepository, times(settings.maxCardCreationRetries() + 1)).create(any());
    }

    @Test
    void testGetCard() {
        var card = generateActiveCard();
        var pan = card.pan();
        when(cardRepository.findByPan(pan)).thenReturn(Optional.of(card));

        var found = cardService.getCard(pan);
        assertEquals(card, found);

        when(cardRepository.findByPan(anyString())).thenReturn(Optional.empty());
        assertThrows(CardNotFoundException.class, () -> cardService.getCard(pan));
    }

    @Test
    void testGetCards() {
        var testCard = generateActiveCard();

        int limit = faker.number().numberBetween(1, 100);
        long offset = faker.number().numberBetween(0, 100);
        CardStatus status = CardStatus.ACTIVE;
        String bin = faker.number().digits(6);
        String issuerId = faker.regexify("[A-Z0-9]{1,10}");
        Instant startDate = Instant.now().minus(1, ChronoUnit.DAYS);
        Instant endDate = Instant.now();

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
    void testPatchCardSuccess() {
        var pan = generatePan();
        var status = CardStatus.EXPIRED;
        var dailyLimit = BigDecimal.valueOf(
            faker.number().numberBetween(0, 10000000)
        );
        var monthlyLimit = BigDecimal.valueOf(
            faker.number().numberBetween(dailyLimit.intValue(), 30000000)
        );
        var availableBalance = BigDecimal.valueOf(
            faker.number().numberBetween(0, 10000000)
        );
        var testCard = generateActiveCardByPan(pan);

        when(cardRepository.findByPanForUpdate(pan))
            .thenReturn(Optional.of(testCard));
        when(cardRepository.update(any()))
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
    void testPatchCardInvalidData() {
        var testCard = generateActiveCard();
        when(cardRepository.findByPanForUpdate(testCard.pan()))
            .thenReturn(Optional.of(testCard));

        assertThrows(IllegalArgumentException.class, () -> {
            cardService.patchCard(
                testCard.pan(),
                CardStatus.ACTIVE,
                BigDecimal.valueOf(-1),
                BigDecimal.valueOf(1),
                BigDecimal.valueOf(1)
            );
        });
        assertThrows(IllegalArgumentException.class, () -> {
            cardService.patchCard(
                testCard.pan(),
                CardStatus.ACTIVE,
                BigDecimal.valueOf(1),
                BigDecimal.valueOf(-1),
                BigDecimal.valueOf(1)
            );
        });
        assertThrows(IllegalArgumentException.class, () -> {
            cardService.patchCard(
                testCard.pan(),
                CardStatus.ACTIVE,
                BigDecimal.valueOf(2),
                BigDecimal.valueOf(1),
                BigDecimal.valueOf(1)
            );
        });
    }

    @Test
    void testDeleteCard() {
        var testCard = generateActiveCard();
        var pan = testCard.pan();
        when(cardRepository.findByPanForUpdate(pan)).thenReturn(Optional.of(testCard));
        when(cardRepository.update(any(Card.class)))
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
        verify(cardRepository, times(1)).update(cardCaptor.capture());
        var deletedCard = cardCaptor.getValue();
        assertEquals(expected, deletedCard);
    }

    @Test
    void testCountCardsFiltered() {
        var amount = 1L;
        CardStatus status = CardStatus.ACTIVE;
        String bin = faker.number().digits(6);
        String issuerId = faker.regexify("[A-Z0-9]{1,10}");
        Instant startDate = Instant.now().minus(1, ChronoUnit.DAYS);
        Instant endDate = Instant.now();

        when(cardRepository.countCardsFiltered(
            status,
            bin,
            issuerId,
            startDate,
            endDate
        )).thenReturn(amount);

        var count = cardService.countCardsFiltered(
            status,
            bin,
            issuerId,
            startDate,
            endDate
        );
        assertEquals(amount, count);
    }

    @Test
    void testCountAllCards() {
        var returnValue = 1L;
        when(cardRepository.countAllCards()).thenReturn(returnValue);
        assertEquals(returnValue, cardService.countAllCards());
    }

    @Test
    void testReserve() {
        var testCard = generateActiveCard();
        var pan = testCard.pan();
        var reserveAmount = BigDecimal.valueOf(
            faker.number().numberBetween(0, testCard.availableBalance().intValue())
        );
        var rrn = faker.number().digits(12);
        when(cardRepository.findByPanForUpdate(pan))
            .thenReturn(Optional.of(testCard));
        when(reservationRepository.isUnique(rrn, pan))
            .thenReturn(true);
        when(reservationRepository.save(reservationCaptor.capture()))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(cardRepository.update(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var expectedCard = new Card(
            testCard.id(),
            testCard.pan(),
            testCard.bin(),
            testCard.cardholderName(),
            testCard.expiryDate(),
            testCard.status(),
            testCard.currencyCode(),
            testCard.dailyLimit(),
            testCard.monthlyLimit(),
            testCard.availableBalance().subtract(reserveAmount),
            testCard.issuerId(),
            testCard.createdAt()
        );

        var card = cardService.reserve(pan, reserveAmount, rrn);
        var actualReservation = reservationCaptor.getValue();
        var expectedReservation = new Reservation(
            actualReservation.id(),
            pan,
            reserveAmount,
            rrn,
            ReservationStatus.RESERVED,
            actualReservation.createdAt(),
            actualReservation.updatedAt()
        );
        assertEquals(expectedReservation, actualReservation);
        assertEquals(expectedCard, card);
        assertThrows(InsufficientFundsException.class, () ->
            cardService.reserve(
                pan,
                BigDecimal.valueOf(Long.MAX_VALUE),
                rrn
            )
        );
        var statuses = Arrays.stream(CardStatus.values())
            .filter(status -> status != CardStatus.ACTIVE && status != CardStatus.DELETED)
            .toArray(CardStatus[]::new);
        when(cardRepository.findByPanForUpdate(pan))
            .thenReturn(Optional.of(generateCard(pan, faker.options().option(statuses))));
        assertThrows(IllegalStateException.class, () -> cardService.reserve(pan, reserveAmount, rrn));
    }

    @Test
    void testRollbackSuccess() {
        var testCard = generateActiveCard();
        var pan = testCard.pan();
        var reserveAmount = BigDecimal.valueOf(
            faker.number().numberBetween(0, testCard.availableBalance().intValue())
        );
        var rrn = faker.number().digits(12);
        var reservation = new Reservation(pan, reserveAmount, rrn);
        when(cardRepository.findByPanForUpdate(pan))
            .thenReturn(Optional.of(testCard));
        when(reservationRepository.findByRrnAndPanForUpdate(rrn, pan))
            .thenReturn(Optional.of(reservation));
        when(reservationRepository.save(reservationCaptor.capture()))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(reservationRollbackRepository.save(reservationRollbackCaptor.capture()))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(cardRepository.update(any()))
            .thenAnswer(invocation -> invocation.getArgument(0));

        var actualCard = cardService.rollback(pan, reserveAmount, rrn);
        var expectedCard = new Card(
            testCard.id(),
            testCard.pan(),
            testCard.bin(),
            testCard.cardholderName(),
            testCard.expiryDate(),
            testCard.status(),
            testCard.currencyCode(),
            testCard.dailyLimit(),
            testCard.monthlyLimit(),
            testCard.availableBalance().add(reserveAmount),
            testCard.issuerId(),
            testCard.createdAt()
        );
        assertEquals(expectedCard, actualCard);

        var actualReservation = reservationCaptor.getValue();
        var expectedReservation = new Reservation(
            reservation.id(),
            pan,
            reserveAmount,
            rrn,
            ReservationStatus.ROLLED_BACK,
            reservation.createdAt(),
            actualReservation.updatedAt()
        );
        assertEquals(expectedReservation, actualReservation);

        var actualRollback = reservationRollbackCaptor.getValue();
        var expectedRollback = new ReservationRollback(
            actualRollback.id(),
            reservation.id(),
            pan,
            reserveAmount,
            rrn,
            actualRollback.createdAt()
        );
        assertEquals(expectedRollback, actualRollback);
    }

    @Test
    void testRollbackWrongPan() {
        var testCard = generateActiveCard();
        var pan = testCard.pan();
        var reserveAmount = BigDecimal.valueOf(
            faker.number().numberBetween(0, testCard.availableBalance().intValue())
        );
        var rrn = faker.number().digits(12);
        var reservation = new Reservation(
            generatePan(),
            reserveAmount,
            rrn
        );
        when(cardRepository.findByPanForUpdate(pan)).thenReturn(Optional.of(testCard));
        when(reservationRepository.findByRrnAndPanForUpdate(rrn, pan)).thenReturn(Optional.of(reservation));
        when(reservationRollbackRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        assertThrows(IllegalArgumentException.class, () -> cardService.rollback(
            pan,
            reserveAmount,
            rrn
        ));
    }

    @Test
    void testBulkUpdateByBin() {
        var bin = generateBin();
        var card1 = generateActiveCardByPan(generatePan());
        var card2 = generateActiveCardByPan(generatePan());

        when(cardRepository.findCardsByBinsForUpdate(List.of(bin)))
            .thenReturn(List.of(card1, card2));
        when(cardRepository.update(any(Card.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        int updated = cardService.bulkUpdateStatus(List.of(bin), null, CardStatus.BLOCKED);

        assertEquals(2, updated);
        verify(cardRepository, times(2)).update(any(Card.class));
    }

    @Test
    void testBulkUpdateByPans() {
        var card1 = generateActiveCardByPan(generatePan());
        var card2 = generateActiveCardByPan(generatePan());

        when(cardRepository.findCardsByPansForUpdate(List.of(card1.pan(), card2.pan())))
            .thenReturn(List.of(card1, card2));
        when(cardRepository.update(any(Card.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        int updated = cardService.bulkUpdateStatus(null, List.of(card1.pan(), card2.pan()), CardStatus.BLOCKED);

        assertEquals(2, updated);
        verify(cardRepository, times(2)).update(any(Card.class));
    }

    @Test
    void bulkUpdateBothNullThrows() {
        assertThrows(IllegalArgumentException.class, () ->
            cardService.bulkUpdateStatus(null, null, CardStatus.BLOCKED));
    }

    @Test
    void bulkUpdateBothThrows() {
        assertThrows(IllegalArgumentException.class, () ->
            cardService.bulkUpdateStatus(List.of(generateBin()), List.of(generatePan()), CardStatus.BLOCKED));
    }
}

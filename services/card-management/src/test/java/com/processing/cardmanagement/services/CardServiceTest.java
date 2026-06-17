package com.processing.cardmanagement.services;

import com.processing.cardmanagement.events.CardEventNotifier;
import com.processing.cardmanagement.exceptions.CardNotFoundException;
import com.processing.cardmanagement.exceptions.InsufficientFundsException;
import com.processing.cardmanagement.models.*;
import com.processing.cardmanagement.options.CardServiceDefaults;
import com.processing.cardmanagement.options.CardServiceDefaultsConfigurationProperties;
import com.processing.cardmanagement.options.CardServiceSettings;
import com.processing.cardmanagement.options.CardServiceSettingsConfigurationProperties;
import com.processing.cardmanagement.repositories.CardRepository;
import com.processing.cardmanagement.repositories.ReservationRepository;
import com.processing.cardmanagement.repositories.ReservationRollbackRepository;
import net.datafaker.Faker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
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

    private final CardServiceSettings settings = new CardServiceSettingsConfigurationProperties(
        "TESTISSUER",
        3,
        10000
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

    private final CardEventNotifier eventNotifier = new CardEventNotifier(List.of());

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
            binIssuerService
        );
    }

    @Test
    void testCreateCard() {
        var bin = faker.number().digits(6);
        var cardholderName = faker.name().fullName().toUpperCase(Locale.ROOT);
        var currencyCode = faker.number().digits(3);
        var dailyLimit = BigDecimal.valueOf(faker.number().numberBetween(0, 10000000));
        var monthlyLimit = BigDecimal.valueOf(faker.number().numberBetween(dailyLimit.intValue(), 30000000));
        var initialBalance = BigDecimal.valueOf(faker.number().numberBetween(0, 10000000));
        var expDate = YearMonth.now().plusYears(settings.cardValidityPeriod());

        when(cardRepository.save(any(Card.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        when(binIssuerService.getIssuerId(bin)).thenReturn(settings.issuerId());

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
        long offset = faker.number().numberBetween(0, 100);
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
        var dailyLimit = BigDecimal.valueOf(
            faker.number().numberBetween(0, 10000000)
        );
        var monthlyLimit = BigDecimal.valueOf(
            faker.number().numberBetween(dailyLimit.intValue(), 30000000)
        );
        var availableBalance = BigDecimal.valueOf(
            faker.number().numberBetween(0, 10000000)
        );
        var testCard = createTestCard(pan);

        when(cardRepository.findByPanForUpdate(pan))
            .thenReturn(Optional.of(testCard));
        when(cardRepository.save(any()))
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
        var pan = generatePan();
        var testCard = createTestCard(pan);
        var reserveAmount = BigDecimal.valueOf(
            faker.number().numberBetween(0, testCard.availableBalance().intValue())
        );
        var rrn = faker.number().digits(12);
        when(cardRepository.findByPanForUpdate(pan))
            .thenReturn(Optional.of(testCard));
        when(reservationRepository.findByRrn(rrn)).thenReturn(Optional.empty());
        when(reservationRepository.save(reservationCaptor.capture()))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(cardRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

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
    }

    @Test
    void testRollbackSuccess() {
        var pan = generatePan();
        var testCard = createTestCard(pan);
        var reserveAmount = BigDecimal.valueOf(
            faker.number().numberBetween(0, testCard.availableBalance().intValue())
        );
        var rrn = faker.number().digits(12);
        var reservation = new Reservation(pan, reserveAmount, rrn);
        when(cardRepository.findByPanForUpdate(pan))
            .thenReturn(Optional.of(testCard));
        when(reservationRepository.findByRrn(rrn)).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(reservationCaptor.capture()))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(reservationRollbackRepository.save(reservationRollbackCaptor.capture()))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(cardRepository.save(any()))
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
        var pan = generatePan();
        var testCard = createTestCard(pan);
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
        when(reservationRepository.findByRrn(rrn)).thenReturn(Optional.of(reservation));
        when(reservationRollbackRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        assertThrows(IllegalArgumentException.class, () -> cardService.rollback(
            pan,
            reserveAmount,
            rrn
        ));
    }

    private Card createTestCard(String pan) {
        return new Card(
            UUID.randomUUID(),
            pan,
            pan.substring(0, 6),
            faker.name().fullName().toUpperCase(Locale.ROOT),
            YearMonth.now().plusYears(settings.cardValidityPeriod()),
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

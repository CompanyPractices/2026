package com.processing.authorization.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

import java.net.URI;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.Instant;
import java.time.YearMonth;
import java.util.UUID;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClient;

import com.processing.authorization.repositories.LimitUsageRepository;
import com.processing.authorization.services.AuthService;
import com.processing.common.dto.authorization.AuthorizationRequest;
import com.processing.common.dto.authorization.AuthorizationResponse;
import com.processing.common.dto.cardmanagement.CardModel;
import com.processing.common.dto.cardmanagement.CardModelStatus;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootTest
public class DBIntegrationTest {
    @Autowired
    private AuthService authService;
    @Autowired
    private LimitUsageRepository limitUsageRepository;
    @Autowired
    private DataSource dataSource;
    @MockitoBean
    private RestClient restClient;

    @Mock
    private RestClient.RequestHeadersUriSpec<?> requestHeadersUriSpec;
    @Mock
    private RestClient.RequestHeadersSpec<?> requestHeadersSpec;
    @Mock
    private RestClient.ResponseSpec responseSpec;
    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;
    @Mock
    private RestClient.RequestBodySpec requestBodySpec;

    private Instant now;
    private AuthorizationRequest correctRequest;
    private AuthorizationRequest correctRequestOther;

    @BeforeEach
    void setUp() {
        Mockito.reset(requestHeadersUriSpec, requestHeadersSpec, responseSpec,
                requestBodyUriSpec, requestBodySpec);

        limitUsageRepository.deleteAll();
        now = Instant.now();
        correctRequest = new AuthorizationRequest(
                "0100",
                "123456",
                "1234567890123456",
                "000000",
                BigDecimal.valueOf(5000),
                "810",
                Instant.parse("2026-06-05T18:12:49.070Z"),
                "T0000001",
                null,
                "M00000000000001",
                "5411",
                "A001",
                "I001");

        correctRequestOther = new AuthorizationRequest(
                "0100",
                "123456",
                "6543210987654321",
                "000000",
                BigDecimal.valueOf(5000),
                "810",
                Instant.parse("2026-06-05T18:12:49.070Z"),
                "T0000001",
                null,
                "M00000000000001",
                "5411",
                "A001",
                "I001");

        // log.debug("Test database URL: {}", getDatabaseUrl());
    }

    private void mockGetCard(CardModel cardToReturn) {
        doReturn(requestHeadersUriSpec).when(restClient).get();
        doReturn(requestHeadersSpec).when(requestHeadersUriSpec).uri(any(URI.class));
        doReturn(responseSpec).when(requestHeadersSpec).retrieve();
        doReturn(responseSpec).when(responseSpec).onStatus(any(), any());
        doReturn(cardToReturn).when(responseSpec).body(CardModel.class);
    }

    private void mockReserveSuccess() {
        doReturn(requestBodyUriSpec).when(restClient).post();
        doReturn(requestBodySpec).when(requestBodyUriSpec).uri(any(URI.class));
        doReturn(requestBodySpec).when(requestBodySpec).contentType(any());
        doReturn(requestBodySpec).when(requestBodySpec).body(any(Object.class));
        doReturn(responseSpec).when(requestBodySpec).retrieve();
        doReturn(responseSpec).when(responseSpec).onStatus(any(), any());
        doReturn(null).when(responseSpec).toBodilessEntity();
    }

    private void mockRollbackSuccess() {
        doReturn(requestBodyUriSpec).when(restClient).post();
        doReturn(requestBodySpec).when(requestBodyUriSpec).uri(any(URI.class));
        doReturn(requestBodySpec).when(requestBodySpec).contentType(any());
        doReturn(requestBodySpec).when(requestBodySpec).body(any(Object.class));
        doReturn(responseSpec).when(requestBodySpec).retrieve();
        doReturn(responseSpec).when(responseSpec).onStatus(any(), any());
        doReturn(null).when(responseSpec).toBodilessEntity();
    }

    // private String getDatabaseUrl() {
    //     try {
    //         return dataSource.getConnection().getMetaData().getURL();
    //     } catch (SQLException e) {
    //         log.warn("Could not determine database URL", e);
    //         return "none";
    //     }
    // }

    @Test
    void authorizeShouldReturnDeclinedWhenCardIsBlocked() {
        CardModel mockCard = createBlockedCardModel();
        mockGetCard(mockCard);
        AuthorizationResponse response = authService.authorize(correctRequest, now);
        assertEquals(AuthorizationResponse.STATUS_DECLINED, response.status());
        assertThat(response.authCode()).isNull();
        assertThat(response.rrn()).isNull();
        assertEquals("CARD_BLOCKED", response.declineReason());
    }

    @Test
    void authorizeShouldReturnDeclinedWhenCardIsExpired() {
        CardModel mockCard = createExpiredCardModel();
        mockGetCard(mockCard);
        AuthorizationResponse response = authService.authorize(correctRequest, now);
        assertEquals(AuthorizationResponse.STATUS_DECLINED, response.status());
        assertEquals("CARD_EXPIRED", response.declineReason());
    }

    @Test
    void authorizeShouldReturnDeclinedWhenCardIsInactive() {
        CardModel mockCard = createInactiveCardModel();
        mockGetCard(mockCard);
        AuthorizationResponse response = authService.authorize(correctRequest, now);
        assertEquals(AuthorizationResponse.STATUS_DECLINED, response.status());
        assertEquals("CARD_INACTIVE", response.declineReason());
    }

    @Test
    void authorizeShouldReturnDeclinedWhenInsufficientFunds() {
        CardModel mockCard = createCardWithLowBalance();
        mockGetCard(mockCard);
        AuthorizationResponse response = authService.authorize(correctRequest, now);
        assertEquals(AuthorizationResponse.STATUS_DECLINED, response.status());
        assertEquals("INSUFFICIENT_FUNDS", response.declineReason());
    }

    @Test
    void authorizeShouldReturnDeclinedWhenExceededMonthlyLimit() {
        CardModel mockCard = createActiveCardModelWithLowMonthlyLimit();
        mockGetCard(mockCard);
        AuthorizationResponse response = authService.authorize(correctRequest, now);
        assertEquals(AuthorizationResponse.STATUS_DECLINED, response.status());
        assertEquals("EXCEEDS_AMOUNT_LIMIT", response.declineReason());
    }

    @Test
    void authorizeShouldReturnDeclinedWhenReservationFails() {
        CardModel mockCard = createActiveCardModel();
        mockGetCard(mockCard);
        AuthorizationResponse response = authService.authorize(correctRequest, now);
        assertEquals(AuthorizationResponse.STATUS_DECLINED, response.status());
        assertEquals("RESERVATION_FAILED", response.declineReason());
    }

    @Test
    void authorizeShouldReturnDeclinedWhenCardExpiredByDate() {
        CardModel mockCard = createCardExpiredByDate();
        mockGetCard(mockCard);
        AuthorizationResponse response = authService.authorize(correctRequest, now);
        assertEquals(AuthorizationResponse.STATUS_DECLINED, response.status());
        assertEquals("CARD_EXPIRED", response.declineReason());
    }

    @Test
    void authorizeShouldBeApproved() {
        CardModel mockCard = createActiveCardModel();
        mockGetCard(mockCard);
        mockReserveSuccess();
        AuthorizationResponse response = authService.authorize(correctRequest, now);
        assertEquals(AuthorizationResponse.STATUS_APPROVED, response.status());
        assertThat(response.authCode()).isNotNull();
        assertThat(response.rrn()).isNotNull();
    }

    @Test
    void authorizeShouldBeApprovedAndPersistToDatabase() {
        CardModel mockCard = createActiveCardModel();
        mockGetCard(mockCard);
        mockReserveSuccess();
        AuthorizationResponse response = authService.authorize(correctRequest, now);
        assertEquals(AuthorizationResponse.STATUS_APPROVED, response.status());
        assertThat(limitUsageRepository.findAll()).hasSize(1);
    }

    // @Test
    // void authorizeShouldBeApprovedForBothPans() {
    //     CardModel mockCard1 = createActiveCardModel();
    //     CardModel mockCard2 = createActiveCardModelOther();
    //     mockGetCard(mockCard1);
    //     mockGetCard(mockCard2);
    //     mockReserveSuccess(); //for mockCard1
    //     mockReserveSuccess(); //for mockCard2
    //     AuthorizationResponse response1 = authService.authorize(correctRequest, now);
    //     AuthorizationResponse response2 = authService.authorize(correctRequestOther, now);
    //     assertEquals(AuthorizationResponse.STATUS_APPROVED, response1.status());
    //     assertEquals(AuthorizationResponse.STATUS_APPROVED, response2.status());
    //     assertThat(limitUsageRepository.findAll()).hasSize(2);
    // }

    private CardModel createActiveCardModel() {
        return new CardModel(
                UUID.randomUUID(),
                "1234567890123456",
                "123456",
                "John Golt",
                YearMonth.of(2026, 12),
                CardModelStatus.ACTIVE,
                "810",
                BigDecimal.valueOf(100000),
                BigDecimal.valueOf(500000),
                BigDecimal.valueOf(10000),
                "I001",
                Instant.now());
    }
    private CardModel createActiveCardModelOther() {
        return new CardModel(
                UUID.randomUUID(),
                "6543210987654321",
                "654321",
                "John Snow",
                YearMonth.of(2026, 12),
                CardModelStatus.ACTIVE,
                "810",
                BigDecimal.valueOf(100000),
                BigDecimal.valueOf(500000),
                BigDecimal.valueOf(10000),
                "I001",
                Instant.now());
    }

    private CardModel createBlockedCardModel() {
        return new CardModel(
                UUID.randomUUID(),
                "1234567890123456",
                "123456",
                "John Golt",
                YearMonth.of(2026, 12),
                CardModelStatus.BLOCKED,
                "810",
                BigDecimal.valueOf(100000),
                BigDecimal.valueOf(500000),
                BigDecimal.valueOf(10000),
                "I001",
                Instant.now());
    }

    private CardModel createExpiredCardModel() {
        return new CardModel(
                UUID.randomUUID(),
                "1234567890123456",
                "123456",
                "John Golt",
                YearMonth.of(2026, 1),
                CardModelStatus.EXPIRED,
                "810",
                BigDecimal.valueOf(100000),
                BigDecimal.valueOf(500000),
                BigDecimal.valueOf(10000),
                "I001",
                Instant.now());
    }

    private CardModel createInactiveCardModel() {
        return new CardModel(
                UUID.randomUUID(),
                "1234567890123456",
                "123456",
                "John Golt",
                YearMonth.of(2029, 1),
                CardModelStatus.INACTIVE,
                "810",
                BigDecimal.valueOf(100000),
                BigDecimal.valueOf(500000),
                BigDecimal.valueOf(10000),
                "I001",
                Instant.now());
    }

    private CardModel createCardWithLowBalance() {
        return new CardModel(
                UUID.randomUUID(),
                "1234567890123456",
                "123456",
                "John Golt",
                YearMonth.of(2026, 12),
                CardModelStatus.ACTIVE,
                "810",
                BigDecimal.valueOf(100000),
                BigDecimal.valueOf(500000),
                BigDecimal.valueOf(1000),
                "I001",
                Instant.now());
    }

    private CardModel createActiveCardModelWithLowMonthlyLimit() {
        return new CardModel(
                UUID.randomUUID(),
                "1234567890123456",
                "123456",
                "John Golt",
                YearMonth.of(2026, 12),
                CardModelStatus.ACTIVE,
                "810",
                BigDecimal.valueOf(100000),
                BigDecimal.valueOf(500),
                BigDecimal.valueOf(10000),
                "I001",
                Instant.now());
    }

    private CardModel createCardExpiredByDate() {
        return new CardModel(
                UUID.randomUUID(),
                "1234567890123456",
                "123456",
                "John Golt",
                YearMonth.of(2006, 12),
                CardModelStatus.ACTIVE,
                "810",
                BigDecimal.valueOf(100000),
                BigDecimal.valueOf(500000),
                BigDecimal.valueOf(10000),
                "I001",
                Instant.now());
    }
}

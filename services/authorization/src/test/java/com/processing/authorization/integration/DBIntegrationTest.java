package com.processing.authorization.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.time.LocalDateTime;
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
import org.springframework.web.reactive.function.client.WebClient;

import com.processing.authorization.repositories.LimitUsageRepository;
import com.processing.authorization.services.AuthService;
import com.processing.common.dto.authorization.AuthorizationRequest;
import com.processing.common.dto.authorization.AuthorizationResponse;
import com.processing.common.dto.cardmanagement.CardModel;
import com.processing.common.dto.cardmanagement.CardModelStatus;

import reactor.core.publisher.Mono;

@SpringBootTest
public class DBIntegrationTest {
    @Autowired
    private AuthService authService;
    @Autowired
    private LimitUsageRepository limitUsageRepository;
    @Autowired
    private DataSource dataSource;
    @MockitoBean
    private WebClient webClient;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;
    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;
    @Mock
    private WebClient.ResponseSpec responseSpec;
    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;
    @Mock
    private WebClient.RequestBodySpec requestBodySpec;

    private LocalDateTime now;
    private AuthorizationRequest correctRequest;

    @BeforeEach
    void setUp() {
        Mockito.reset(requestHeadersUriSpec, requestHeadersSpec, responseSpec,
                requestBodyUriSpec, requestBodySpec);

        limitUsageRepository.deleteAll();
        now = LocalDateTime.now();
        correctRequest = new AuthorizationRequest(
                "0100",
                "123456",
                "1234567890123456",
                "000000",
                5000L,
                "810",
                "2026-06-05T18:12:49.070",
                "T0000001",
                null,
                "M00000000000001",
                "5411",
                "A001",
                "I001");
    }

    private void mockWebClientGetCard(CardModel cardToReturn) {
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(CardModel.class))
                .thenReturn(Mono.just(cardToReturn));
    }

    private void mockWebClientReserveSuccess() {
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class))
                .thenReturn(Mono.just("RESERVED"));
    }

    @Test
    void whatDatabase() throws SQLException {
        String url = dataSource.getConnection().getMetaData().getURL();
        System.out.println("Database URL: " + url);
    }

    @Test
    void authorizeShouldReturnDeclinedWhenCardIsBlocked() {
        CardModel mockCard = createBlockedCardModel();
        mockWebClientGetCard(mockCard);
        AuthorizationResponse response = authService.authorize(correctRequest, now);
        assertEquals(AuthorizationResponse.STATUS_DECLINED, response.status());
        assertThat(response.authCode()).isNull();
        assertThat(response.rrn()).isNull();
        assertEquals("CARD_BLOCKED", response.declineReason());
    }

    @Test
    void authorizeShouldReturnDeclinedWhenCardIsExpired() {
        CardModel mockCard = createExpiredCardModel();
        mockWebClientGetCard(mockCard);
        AuthorizationResponse response = authService.authorize(correctRequest, now);
        assertEquals(AuthorizationResponse.STATUS_DECLINED, response.status());
        assertEquals("CARD_EXPIRED", response.declineReason());
    }

    @Test
    void authorizeShouldReturnDeclinedWhenCardIsInactive() {
        CardModel mockCard = createInactiveCardModel();
        mockWebClientGetCard(mockCard);
        AuthorizationResponse response = authService.authorize(correctRequest, now);
        assertEquals(AuthorizationResponse.STATUS_DECLINED, response.status());
        assertEquals("CARD_INACTIVE", response.declineReason());
    }

    @Test
    void authorizeShouldReturnDeclinedWhenInsufficientFunds() {
        CardModel mockCard = createCardWithLowBalance();
        mockWebClientGetCard(mockCard);
        AuthorizationResponse response = authService.authorize(correctRequest, now);
        assertEquals(AuthorizationResponse.STATUS_DECLINED, response.status());
        assertEquals("INSUFFICIENT_FUNDS", response.declineReason());
    }

    void authorizeShouldReturnDeclinedWhenExceededMonthlyLimit() {
        CardModel mockCard = createActiveCardModelWithLowMonthlyLimit();
        mockWebClientGetCard(mockCard);
        AuthorizationResponse response = authService.authorize(correctRequest, now);
        assertEquals(AuthorizationResponse.STATUS_DECLINED, response.status());
        assertEquals("EXCEEDS_AMOUNT_LIMIT", response.declineReason());
    }

    @Test
    void authorizeShouldReturnDeclinedWhenReservationFails() throws Exception {
        CardModel mockCard = createActiveCardModel();
        mockWebClientGetCard(mockCard);
        AuthorizationResponse response = authService.authorize(correctRequest, now);
        assertEquals(AuthorizationResponse.STATUS_DECLINED, response.status());
        assertEquals("RESERVATION_FAILED", response.declineReason());
    }

    @Test
    void authorizeShouldReturnDeclinedWhenCardExpiredByDate() {
        CardModel mockCard = createCardExpiredByDate();
        mockWebClientGetCard(mockCard);
        AuthorizationResponse response = authService.authorize(correctRequest, now);
        assertEquals(AuthorizationResponse.STATUS_DECLINED, response.status());
        assertEquals("CARD_EXPIRED", response.declineReason());
    }

    @Test
    void authorizeShouldBeApproved() {
        CardModel mockCard = createActiveCardModel();
        mockWebClientGetCard(mockCard);
        mockWebClientReserveSuccess();
        AuthorizationResponse response = authService.authorize(correctRequest, now);
        assertEquals(AuthorizationResponse.STATUS_APPROVED, response.status());
        assertThat(response.authCode()).isNotNull();
        assertThat(response.rrn()).isNotNull();
    }

    @Test
    void authorizeShouldBeApprovedAndPersistToDatabase() {
        CardModel mockCard = createActiveCardModel();
        mockWebClientGetCard(mockCard);
        mockWebClientReserveSuccess();
        AuthorizationResponse response = authService.authorize(correctRequest, now);
        assertEquals(AuthorizationResponse.STATUS_APPROVED, response.status());
        assertThat(limitUsageRepository.findAll()).hasSize(1);
    }

    private CardModel createActiveCardModel() {
        return new CardModel(
                UUID.randomUUID(),
                "1234567890123456",
                "123456",
                "John Golt",
                YearMonth.of(2026, 12),
                CardModelStatus.ACTIVE,
                "810",
                100000L,
                500000L,
                10000L,
                "I001",
                now);
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
                100000L,
                500000L,
                10000L,
                "I001",
                now);
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
                100000L,
                500000L,
                10000L,
                "I001",
                now);
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
                100000L,
                500000L,
                10000L,
                "I001",
                now);
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
                100000L,
                500000L,
                1000L,
                "I001",
                now);
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
                100000L,
                500L,
                10000L,
                "I001",
                now);
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
                100000L,
                500000L,
                10000L,
                "I001",
                now);
    }
}

package com.processing.authorization.service;

import com.processing.authorization.dto.AuthorizationRequest;
import com.processing.authorization.dto.AuthorizationResponse;
import com.processing.authorization.dto.CardResponse;
import com.processing.authorization.enums.AuthorizationRequestStatus;
import com.processing.authorization.enums.CardStatus;
import com.processing.authorization.exceptions.ServiceUnavaliableException;
import com.processing.authorization.services.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private AuthService authService;

    private AuthorizationRequest correctRequest;
    private CardResponse activeCardResponse;

    @BeforeEach
    void setUp() {
        WebClient webClient = WebClient.create();
        authService = new AuthService(webClient);

        correctRequest = new AuthorizationRequest(
                "0100",
                "123456",
                "1234567890123456",
                "000000",
                5000,
                "810",
                LocalDateTime.now(),
                "T0000001",
                null,
                "M00000000000001",
                "5411",
                "A001",
                "I001");

        activeCardResponse = new CardResponse(
                UUID.randomUUID(),
                "1234567890123456",
                "123456",
                "John Golt",
                LocalDate.now().plusMonths(6),
                CardStatus.ACTIVE,
                "810",
                100000L,
                500000L,
                10000L,
                "I001",
                LocalDate.now());
    }

    @Test
    void authorizeApprovedWhenAllChecksPassed() throws Exception {
        AuthService spyService = spy(authService);

        doReturn(activeCardResponse).when(spyService).getCard(anyString());
        doNothing().when(spyService).reserve(anyInt(), anyString(), anyString());

        AuthorizationResponse response = spyService.authorize(correctRequest);

        assertThat(response.getStatus()).isEqualTo(AuthorizationRequestStatus.APPROVED);
        assertThat(response.getResponseCode()).isEqualTo("00");
        assertThat(response.getRrn()).isNotNull();
        assertThat(response.getAuthCode()).isNotNull();
        assertThat(response.getDeclineReason()).isNull();

        verify(spyService, times(1)).getCard(correctRequest.getPan());
        verify(spyService, times(1)).reserve(eq(correctRequest.getAmount()), anyString(), eq(correctRequest.getPan()));
    }

    @Test
    void authorizeReturnServiceUnavailableWhenGetCardThrowsException() throws Exception {
        AuthService spyService = spy(authService);
        ServiceUnavaliableException cause = new ServiceUnavaliableException("Card Management service unavaliable");
        WebClientResponseException exception = new WebClientResponseException(
                500, "Internal service error", null, null, null);
        exception.initCause(cause);
        doThrow(exception).when(spyService).getCard(anyString());

        AuthorizationResponse response = spyService.authorize(correctRequest);

        assertThat(response.getStatus()).isEqualTo(AuthorizationRequestStatus.DECLINED);
        assertThat(response.getResponseCode()).isEqualTo("96");
        assertThat(response.getDeclineReason()).isEqualTo("SERVICE_UNAVAILABLE");
        verify(spyService, never()).reserve(anyInt(), anyString(), anyString());
    }

    @Test
    void authorizeDeclineWhenCardExpired() throws Exception {
        CardResponse expiredCard = new CardResponse(
                activeCardResponse.getId(),
                activeCardResponse.getPan(),
                activeCardResponse.getBin(),
                activeCardResponse.getCardholderName(),
                activeCardResponse.getExpiryDate(),
                CardStatus.EXPIRED,
                activeCardResponse.getCurrencyCode(),
                activeCardResponse.getDailyLimit(),
                activeCardResponse.getMonthlyLimit(),
                activeCardResponse.getAvailableBalance(),
                activeCardResponse.getIssuerId(),
                activeCardResponse.getCreatedAt());
        AuthService spyService = spy(authService);
        doReturn(expiredCard).when(spyService).getCard(anyString());

        AuthorizationResponse response = spyService.authorize(correctRequest);

        assertThat(response.getResponseCode()).isEqualTo("54");
        assertThat(response.getDeclineReason()).isEqualTo("CARD_EXPIRED");
        verify(spyService, never()).reserve(anyInt(), anyString(), anyString());
    }

    @Test
    void authorizeDeclineWhenCardBlocked() throws Exception {
        CardResponse blockedCard = new CardResponse(
                activeCardResponse.getId(),
                activeCardResponse.getPan(),
                activeCardResponse.getBin(),
                activeCardResponse.getCardholderName(),
                activeCardResponse.getExpiryDate(),
                CardStatus.BLOCKED,
                activeCardResponse.getCurrencyCode(),
                activeCardResponse.getDailyLimit(),
                activeCardResponse.getMonthlyLimit(),
                activeCardResponse.getAvailableBalance(),
                activeCardResponse.getIssuerId(),
                activeCardResponse.getCreatedAt());
        AuthService spyService = spy(authService);
        doReturn(blockedCard).when(spyService).getCard(anyString());

        AuthorizationResponse response = spyService.authorize(correctRequest);

        assertThat(response.getResponseCode()).isEqualTo("05");
        assertThat(response.getDeclineReason()).isEqualTo("CARD_BLOCKED");
    }

    @Test
    void authorizeDeclineWhenCardInactive() throws Exception {
        CardResponse inactiveCard = new CardResponse(
                activeCardResponse.getId(),
                activeCardResponse.getPan(),
                activeCardResponse.getBin(),
                activeCardResponse.getCardholderName(),
                activeCardResponse.getExpiryDate(),
                CardStatus.INACTIVE,
                activeCardResponse.getCurrencyCode(),
                activeCardResponse.getDailyLimit(),
                activeCardResponse.getMonthlyLimit(),
                activeCardResponse.getAvailableBalance(),
                activeCardResponse.getIssuerId(),
                activeCardResponse.getCreatedAt());
        AuthService spyService = spy(authService);
        doReturn(inactiveCard).when(spyService).getCard(anyString());

        AuthorizationResponse response = spyService.authorize(correctRequest);

        assertThat(response.getResponseCode()).isEqualTo("05");
        assertThat(response.getDeclineReason()).isEqualTo("CARD_INACTIVE");
    }

    @Test
    void authorizeDeclineWhenCardStatusUnknown() throws Exception {
        CardResponse unknownStatusCard = new CardResponse(
                activeCardResponse.getId(),
                activeCardResponse.getPan(),
                activeCardResponse.getBin(),
                activeCardResponse.getCardholderName(),
                activeCardResponse.getExpiryDate(),
                null,
                activeCardResponse.getCurrencyCode(), activeCardResponse.getDailyLimit(),
                activeCardResponse.getMonthlyLimit(), activeCardResponse.getAvailableBalance(),
                activeCardResponse.getIssuerId(), activeCardResponse.getCreatedAt());
        AuthService spyService = spy(authService);
        doReturn(unknownStatusCard).when(spyService).getCard(anyString());

        AuthorizationResponse response = spyService.authorize(correctRequest);

        assertThat(response.getResponseCode()).isEqualTo("05");
        assertThat(response.getDeclineReason()).isEqualTo("UNKNOWN_REASON");
    }

    @Test
    void authorize_shouldDecline_whenExpiryDateInPastEvenIfCardActive() throws Exception {
        CardResponse activeButExpiredCard = new CardResponse(
                activeCardResponse.getId(),
                activeCardResponse.getPan(),
                activeCardResponse.getBin(),
                activeCardResponse.getCardholderName(),
                LocalDate.now().minusDays(1),
                CardStatus.ACTIVE, activeCardResponse.getCurrencyCode(),
                activeCardResponse.getDailyLimit(),
                activeCardResponse.getMonthlyLimit(),
                activeCardResponse.getAvailableBalance(),
                activeCardResponse.getIssuerId(),
                activeCardResponse.getCreatedAt());
        AuthService spyService = spy(authService);
        doReturn(activeButExpiredCard).when(spyService).getCard(anyString());

        AuthorizationResponse response = spyService.authorize(correctRequest);

        assertThat(response.getResponseCode()).isEqualTo("54");
        assertThat(response.getDeclineReason()).isEqualTo("CARD_EXPIRED");
        verify(spyService, never()).reserve(anyInt(), anyString(), anyString());
    }

    @Test
    void authorizeDeclineWhenInsufficientFunds() throws Exception {
        CardResponse lowBalanceCard = new CardResponse(
                activeCardResponse.getId(),
                activeCardResponse.getPan(),
                activeCardResponse.getBin(),
                activeCardResponse.getCardholderName(),
                activeCardResponse.getExpiryDate(),
                CardStatus.ACTIVE, activeCardResponse.getCurrencyCode(),
                activeCardResponse.getDailyLimit(),
                activeCardResponse.getMonthlyLimit(),
                1000L,
                activeCardResponse.getIssuerId(),
                activeCardResponse.getCreatedAt());
        AuthService spyService = spy(authService);
        doReturn(lowBalanceCard).when(spyService).getCard(anyString());

        AuthorizationResponse response = spyService.authorize(correctRequest);

        assertThat(response.getResponseCode()).isEqualTo("51");
        assertThat(response.getDeclineReason()).isEqualTo("INSUFFICIENT_FUNDS");
        verify(spyService, never()).reserve(anyInt(), anyString(), anyString());
    }

    @Test
    void authorizeDeclineWhenReserveThrowsException() throws Exception {
        AuthService spyService = spy(authService);
        doReturn(activeCardResponse).when(spyService).getCard(anyString());
        doThrow(new Exception("Reserve failed")).when(spyService).reserve(anyInt(), anyString(), anyString());

        AuthorizationResponse response = spyService.authorize(correctRequest);

        assertThat(response.getResponseCode()).isEqualTo("96");
        assertThat(response.getDeclineReason()).isEqualTo("RESERVATION_FAILED");
        verify(spyService, times(1)).reserve(eq(correctRequest.getAmount()), anyString(), eq(correctRequest.getPan()));
    }

    @Test
    void generateRRNReturnUniqueValues() {
        String rrn1 = authService.generateRRN();
        String rrn2 = authService.generateRRN();

        assertThat(rrn1).isNotBlank();
        assertThat(rrn2).isNotBlank();
        assertThat(rrn1).isNotEqualTo(rrn2);
        assertThat(rrn1).hasSize(12);
        assertThat(rrn2).hasSize(12);
    }

    @Test
    void generateAuthCode_shouldReturnSixDigitAlphanumeric() {
        String code = authService.generateAuthCode();

        assertThat(code).hasSize(6);
        assertThat(code).matches("[0-9A-Z]{6}");
    }
}

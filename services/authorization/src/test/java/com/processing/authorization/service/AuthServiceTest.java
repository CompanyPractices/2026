package com.processing.authorization.service;

import com.processing.common.dto.authorization.AuthorizationRequest;
import com.processing.common.dto.authorization.AuthorizationResponse;
import com.processing.common.dto.cardmanagement.CardModel;
import com.processing.authorization.entities.LimitUsage;
import com.processing.authorization.repositories.LimitUsageRepository;
import com.processing.authorization.exceptions.ServiceUnavailableException;
import com.processing.authorization.services.AuthService;
import com.processing.common.dto.cardmanagement.CardModelStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import static com.processing.authorization.constants.DeclineOutcome.*;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private AuthService authService;

    private AuthorizationRequest correctRequest;

    private CardModel activeCardResponse;

    @Mock
    private LimitUsageRepository limitUsageRepository;

    @BeforeEach
    void setUp() {
        WebClient webClient = WebClient.create();
        authService = new AuthService(webClient, limitUsageRepository);

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

        activeCardResponse = new CardModel(
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
                LocalDateTime.now());
    }

    @Test
    void authorizeApprovedWhenAllChecksPassed() throws Exception {
        AuthService spyService = spy(authService);

        doReturn(activeCardResponse).when(spyService).getCard(anyString());
        doNothing().when(spyService).reserve(anyLong(), anyString(), anyString());
        when(limitUsageRepository.findByPanAndUsageDate(anyString(), any(LocalDate.class)))
                .thenReturn(Optional.empty());
        when(limitUsageRepository.sumMonthlyAmountByPanAndMonth(anyString(), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(0L);

        AuthorizationResponse response = spyService.authorize(correctRequest, LocalDateTime.now());

        assertThat(response.status()).isEqualTo(AuthorizationResponse.STATUS_APPROVED);
        assertThat(response.responseCode()).isEqualTo(AuthorizationResponse.CODE_APPROVED);
        assertThat(response.rrn()).isNotNull();
        assertThat(response.authCode()).isNotNull();
        assertThat(response.declineReason()).isNull();

        verify(spyService, times(1)).getCard(correctRequest.pan());
        verify(spyService, times(1)).reserve(eq(correctRequest.amount()), anyString(), eq(correctRequest.pan()));
    }

    @Test
    void authorizeReturnServiceUnavailableWhenGetCardThrowsException() throws Exception {
        AuthService spyService = spy(authService);
        ServiceUnavailableException cause = new ServiceUnavailableException("Card Management service unavaliable");
        WebClientResponseException exception = new WebClientResponseException(
                500, "Internal service error", null, null, null);
        exception.initCause(cause);
        doThrow(exception).when(spyService).getCard(anyString());

        AuthorizationResponse response = spyService.authorize(correctRequest, LocalDateTime.now());

        assertThat(response.status()).isEqualTo(AuthorizationResponse.STATUS_DECLINED);
        assertThat(response.responseCode()).isEqualTo(SERVICE_UNAVAILABLE.code());
        assertThat(response.declineReason()).isEqualTo(SERVICE_UNAVAILABLE.reason());
        verify(spyService, never()).reserve(anyLong(), anyString(), anyString());
    }

    @Test
    void authorizeDeclineWhenCardExpired() throws Exception {
        CardModel expiredCard = new CardModel(
                activeCardResponse.id(),
                activeCardResponse.pan(),
                activeCardResponse.bin(),
                activeCardResponse.cardholderName(),
                activeCardResponse.expiryDate(),
            CardModelStatus.EXPIRED,
                activeCardResponse.currencyCode(),
                activeCardResponse.dailyLimit(),
                activeCardResponse.monthlyLimit(),
                activeCardResponse.availableBalance(),
                activeCardResponse.issuerId(),
                activeCardResponse.createdAt());
        AuthService spyService = spy(authService);
        doReturn(expiredCard).when(spyService).getCard(anyString());

        AuthorizationResponse response = spyService.authorize(correctRequest, LocalDateTime.now());

        assertThat(response.responseCode()).isEqualTo(CARD_EXPIRED.code());
        assertThat(response.declineReason()).isEqualTo(CARD_EXPIRED.reason());
        verify(spyService, never()).reserve(anyLong(), anyString(), anyString());
    }

    @Test
    void authorizeDeclineWhenCardBlocked() throws Exception {
        CardModel blockedCard = new CardModel(
                activeCardResponse.id(),
                activeCardResponse.pan(),
                activeCardResponse.bin(),
                activeCardResponse.cardholderName(),
                activeCardResponse.expiryDate(),
                CardModelStatus.BLOCKED,
                activeCardResponse.currencyCode(),
                activeCardResponse.dailyLimit(),
                activeCardResponse.monthlyLimit(),
                activeCardResponse.availableBalance(),
                activeCardResponse.issuerId(),
                activeCardResponse.createdAt());
        AuthService spyService = spy(authService);
        doReturn(blockedCard).when(spyService).getCard(anyString());

        AuthorizationResponse response = spyService.authorize(correctRequest, LocalDateTime.now());

        assertThat(response.responseCode()).isEqualTo(CARD_BLOCKED.code());
        assertThat(response.declineReason()).isEqualTo(CARD_BLOCKED.reason());
    }

    @Test
    void authorizeDeclineWhenCardInactive() throws Exception {
        CardModel inactiveCard = new CardModel(
                activeCardResponse.id(),
                activeCardResponse.pan(),
                activeCardResponse.bin(),
                activeCardResponse.cardholderName(),
                activeCardResponse.expiryDate(),
                CardModelStatus.INACTIVE,
                activeCardResponse.currencyCode(),
                activeCardResponse.dailyLimit(),
                activeCardResponse.monthlyLimit(),
                activeCardResponse.availableBalance(),
                activeCardResponse.issuerId(),
                activeCardResponse.createdAt());
        AuthService spyService = spy(authService);
        doReturn(inactiveCard).when(spyService).getCard(anyString());

        AuthorizationResponse response = spyService.authorize(correctRequest, LocalDateTime.now());

        assertThat(response.responseCode()).isEqualTo(CARD_INACTIVE.code());
        assertThat(response.declineReason()).isEqualTo(CARD_INACTIVE.reason());
    }

    @Test
    void authorizeDeclineWhenCardStatusUnknown() throws Exception {
        CardModel unknownStatusCard = new CardModel(
                activeCardResponse.id(),
                activeCardResponse.pan(),
                activeCardResponse.bin(),
                activeCardResponse.cardholderName(),
                activeCardResponse.expiryDate(),
                null,
                activeCardResponse.currencyCode(),
                activeCardResponse.dailyLimit(),
                activeCardResponse.monthlyLimit(),
                activeCardResponse.availableBalance(),
                activeCardResponse.issuerId(),
                activeCardResponse.createdAt());
        AuthService spyService = spy(authService);
        doReturn(unknownStatusCard).when(spyService).getCard(anyString());

        AuthorizationResponse response = spyService.authorize(correctRequest, LocalDateTime.now());

        assertThat(response.responseCode()).isEqualTo(UNKNOWN_REASON.code());
        assertThat(response.declineReason()).isEqualTo(UNKNOWN_REASON.reason());
    }

    @Test
    void authorize_shouldDecline_whenExpiryDateInPastEvenIfCardActive() throws Exception {
        YearMonth expiredDate = YearMonth.of(2023, 12);

        CardModel activeButExpiredCard = new CardModel(
                activeCardResponse.id(),
                activeCardResponse.pan(),
                activeCardResponse.bin(),
                activeCardResponse.cardholderName(),
                expiredDate,
                CardModelStatus.ACTIVE,
                activeCardResponse.currencyCode(),
                activeCardResponse.dailyLimit(),
                activeCardResponse.monthlyLimit(),
                activeCardResponse.availableBalance(),
                activeCardResponse.issuerId(),
                activeCardResponse.createdAt());
        AuthService spyService = spy(authService);
        doReturn(activeButExpiredCard).when(spyService).getCard(anyString());

        AuthorizationResponse response = spyService.authorize(correctRequest, LocalDateTime.now());

        assertThat(response.responseCode()).isEqualTo(CARD_EXPIRED.code());
        assertThat(response.declineReason()).isEqualTo(CARD_EXPIRED.reason());
        verify(spyService, never()).reserve(anyLong(), anyString(), anyString());
    }

    @Test
    void authorizeDeclineWhenInsufficientFunds() throws Exception {
        CardModel lowBalanceCard = new CardModel(
                activeCardResponse.id(),
                activeCardResponse.pan(),
                activeCardResponse.bin(),
                activeCardResponse.cardholderName(),
                activeCardResponse.expiryDate(),
                CardModelStatus.ACTIVE,
                activeCardResponse.currencyCode(),
                activeCardResponse.dailyLimit(),
                activeCardResponse.monthlyLimit(),
                1000L,
                activeCardResponse.issuerId(),
                activeCardResponse.createdAt());
        AuthService spyService = spy(authService);
        doReturn(lowBalanceCard).when(spyService).getCard(anyString());

        AuthorizationResponse response = spyService.authorize(correctRequest, LocalDateTime.now());

        assertThat(response.responseCode()).isEqualTo(INSUFFICIENT_FUNDS.code());
        assertThat(response.declineReason()).isEqualTo(INSUFFICIENT_FUNDS.reason());
        verify(spyService, never()).reserve(anyLong(), anyString(), anyString());
    }

    @Test
    void authorizeDeclineWhenReserveThrowsException() throws Exception {
        AuthService spyService = spy(authService);
        doReturn(activeCardResponse).when(spyService).getCard(anyString());
        doThrow(new Exception("Reserve failed")).when(spyService).reserve(anyLong(), anyString(), anyString());

        AuthorizationResponse response = spyService.authorize(correctRequest, LocalDateTime.now());

        assertThat(response.responseCode()).isEqualTo(RESERVATION_FAILED.code());
        assertThat(response.declineReason()).isEqualTo(RESERVATION_FAILED.reason());
        verify(spyService, times(1)).reserve(eq(correctRequest.amount()), anyString(), eq(correctRequest.pan()));
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

    @Test
    void authorizeApprovedWhenDailyLimitNotReached() throws Exception {
        AuthService spyService = spy(authService);

        doReturn(activeCardResponse).when(spyService).getCard(anyString());
        doNothing().when(spyService).reserve(anyLong(), anyString(), anyString());

        LimitUsage usage = new LimitUsage();
        usage.setDailyAmount(50000L);
        usage.setMonthlyAmount(200000L);
        when(limitUsageRepository.findByPanAndUsageDate(anyString(), any(LocalDate.class)))
                .thenReturn(Optional.of(usage));
        when(limitUsageRepository.sumMonthlyAmountByPanAndMonth(anyString(), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(200000L);

        AuthorizationResponse response = spyService.authorize(correctRequest, LocalDateTime.now());

        assertThat(response.status()).isEqualTo(AuthorizationResponse.STATUS_APPROVED);
        assertThat(response.responseCode()).isEqualTo(AuthorizationResponse.CODE_APPROVED);
        assertThat(response.rrn()).isNotNull();
        assertThat(response.authCode()).isNotNull();
        assertThat(response.declineReason()).isNull();
    }

    @Test
    void authorizeDeclineWhenDailyLimitReached() throws Exception {
        AuthService spyService = spy(authService);

        doReturn(activeCardResponse).when(spyService).getCard(anyString());

        LimitUsage usage = new LimitUsage();
        usage.setDailyAmount(96000L);
        usage.setMonthlyAmount(200000L);
        when(limitUsageRepository.findByPanAndUsageDate(anyString(), any(LocalDate.class)))
                .thenReturn(Optional.of(usage));

        AuthorizationResponse response = spyService.authorize(correctRequest, LocalDateTime.now());

        assertThat(response.status()).isEqualTo(AuthorizationResponse.STATUS_DECLINED);
        assertThat(response.responseCode()).isEqualTo(EXCEEDS_AMOUNT_LIMIT.code());
        assertThat(response.declineReason()).isEqualTo(EXCEEDS_AMOUNT_LIMIT.reason());
        verify(spyService, never()).reserve(anyLong(), anyString(), anyString());
    }

    @Test
    void authorizeDeclineWhenMonthlyLimitReached() throws Exception {
        AuthService spyService = spy(authService);

        doReturn(activeCardResponse).when(spyService).getCard(anyString());

        when(limitUsageRepository.findByPanAndUsageDate(anyString(), any(LocalDate.class)))
                .thenReturn(Optional.empty());
        when(limitUsageRepository.sumMonthlyAmountByPanAndMonth(anyString(), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(496000L);

        AuthorizationResponse response = spyService.authorize(correctRequest, LocalDateTime.now());

        assertThat(response.status()).isEqualTo(AuthorizationResponse.STATUS_DECLINED);
        assertThat(response.responseCode()).isEqualTo(EXCEEDS_AMOUNT_LIMIT.code());
        assertThat(response.declineReason()).isEqualTo(EXCEEDS_AMOUNT_LIMIT.reason());
        verify(spyService, never()).reserve(anyLong(), anyString(), anyString());
    }

    @Test
    void maskPAN_ShouldMaskMiddleEightCharacters() {
        String input = "4000001234560001";
        String result = authService.maskPAN(input);

        assertEquals("4000********0001", result);
        assertEquals(16, result.length());
    }

    @Test
    void maskPAN_ShouldReturnInvalidPanOnShortString() {
        String input = "4000";
        String result = authService.maskPAN(input);

        assertEquals("INVALID_PAN", result);
    }

    @Test
    void maskPAN_ShouldReturnInvalidPanOnEmptyString() {
        String input = null;
        String result = authService.maskPAN(input);

        assertEquals("INVALID_PAN", result);
    }

    @Test
    void maskPAN_ShouldPreserveOriginalLength() {
        String input = "4000001234560001";
        String result = authService.maskPAN(input);

        assertEquals(input.length(), result.length());
    }
}

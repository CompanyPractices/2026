package com.processing.authorization.service;

import com.processing.authorization.events.AuthorizationEventNotifier;
import com.processing.authorization.client.CardManagementClient;
import com.processing.authorization.exceptions.*;
import com.processing.common.dto.authorization.AuthorizationRequest;
import com.processing.common.dto.authorization.AuthorizationResponse;
import com.processing.common.dto.authorization.RollbackRequest;
import com.processing.common.dto.authorization.RollbackResponse;
import com.processing.common.dto.cardmanagement.CardModel;
import com.processing.authorization.repositories.LimitUsageRepository;
import com.processing.authorization.services.AuthServiceImpl;
import com.processing.common.dto.cardmanagement.CardModelStatus;
import com.processing.common.utils.MaskPan;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;
import static com.processing.authorization.constants.DeclineOutcome.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @InjectMocks
    private AuthServiceImpl authService;

    private AuthorizationRequest correctRequest;

    private CardModel activeCardResponse;

    private RollbackRequest rollbackRequest;

    @Mock
    private LimitUsageRepository limitUsageRepository;
    @Mock
    private RestClient restClient;
    @Mock
    private AuthorizationEventNotifier eventNotifier;

    @Mock
    private CardManagementClient cardManagementClient;

    @BeforeEach
    void setUp() {
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

        activeCardResponse = new CardModel(
                UUID.randomUUID(),
                "1234567890123456",
                "123456",
                "John Golt",
                YearMonth.of(2026, 12),
                CardModelStatus.ACTIVE,
                "810",
                BigDecimal.valueOf(100000),
                BigDecimal.valueOf(500000L),
                BigDecimal.valueOf(10000),
                "I001",
                Instant.now());

        rollbackRequest = new RollbackRequest(
                "123456789012",
                "1234567890123456",
                BigDecimal.valueOf(5000));
    }

    @Test
    void authorizeApprovedWhenAllChecksPassed() {
        doReturn(activeCardResponse).when(cardManagementClient).getCard(anyString());
        doNothing().when(cardManagementClient).reserve(any(BigDecimal.class), anyString(), anyString());
        when(limitUsageRepository.upsertLimitUsage(anyString(), any(LocalDate.class), any(BigDecimal.class),
                any(BigDecimal.class), any(BigDecimal.class)))
                .thenReturn(1);

        AuthorizationResponse response = authService.authorize(correctRequest, Instant.now());

        assertThat(response.status()).isEqualTo(AuthorizationResponse.STATUS_APPROVED);
        assertThat(response.responseCode()).isEqualTo(AuthorizationResponse.CODE_APPROVED);
        assertThat(response.rrn()).isNotNull();
        assertThat(response.authCode()).isNotNull();
        assertThat(response.declineReason()).isNull();

        verify(cardManagementClient, times(1)).getCard(correctRequest.pan());
        verify(cardManagementClient, times(1)).reserve(eq(correctRequest.amount()), anyString(), eq(correctRequest.pan()));
    }

    @Test
    void authorizeReturnServiceUnavailableWhenGetCardThrowsException() {
        doThrow(new ServiceUnavailableException("Card Management service unavailable"))
                .when(cardManagementClient).getCard(anyString());

        AuthorizationResponse response = authService.authorize(correctRequest, Instant.now());

        assertThat(response.status()).isEqualTo(AuthorizationResponse.STATUS_DECLINED);
        assertThat(response.responseCode()).isEqualTo(SERVICE_UNAVAILABLE.code());
        assertThat(response.declineReason()).isEqualTo(SERVICE_UNAVAILABLE.reason());
        verify(cardManagementClient, never()).reserve(any(BigDecimal.class), anyString(), anyString());
    }

    @Test
    void authorizeDeclineWhenCardExpired() {
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
        doReturn(expiredCard).when(cardManagementClient).getCard(anyString());

        AuthorizationResponse response = authService.authorize(correctRequest, Instant.now());

        assertThat(response.responseCode()).isEqualTo(CARD_EXPIRED.code());
        assertThat(response.declineReason()).isEqualTo(CARD_EXPIRED.reason());
        verify(cardManagementClient, never()).reserve(any(BigDecimal.class), anyString(), anyString());
    }

    @Test
    void authorizeDeclineWhenCardBlocked() {
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
        doReturn(blockedCard).when(cardManagementClient).getCard(anyString());

        AuthorizationResponse response = authService.authorize(correctRequest, Instant.now());

        assertThat(response.responseCode()).isEqualTo(CARD_BLOCKED.code());
        assertThat(response.declineReason()).isEqualTo(CARD_BLOCKED.reason());
    }

    @Test
    void authorizeDeclineWhenCardInactive() {
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
        doReturn(inactiveCard).when(cardManagementClient).getCard(anyString());

        AuthorizationResponse response = authService.authorize(correctRequest, Instant.now());

        assertThat(response.responseCode()).isEqualTo(CARD_INACTIVE.code());
        assertThat(response.declineReason()).isEqualTo(CARD_INACTIVE.reason());
    }

    @Test
    void authorizeDeclineWhenCardStatusUnknown() {
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
        doReturn(unknownStatusCard).when(cardManagementClient).getCard(anyString());

        AuthorizationResponse response = authService.authorize(correctRequest, Instant.now());

        assertThat(response.responseCode()).isEqualTo(UNKNOWN_REASON.code());
        assertThat(response.declineReason()).isEqualTo(UNKNOWN_REASON.reason());
    }

    @Test
    void authorize_shouldDecline_whenExpiryDateInPastEvenIfCardActive() {
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
        doReturn(activeButExpiredCard).when(cardManagementClient).getCard(anyString());

        AuthorizationResponse response = authService.authorize(correctRequest, Instant.now());

        assertThat(response.responseCode()).isEqualTo(CARD_EXPIRED.code());
        assertThat(response.declineReason()).isEqualTo(CARD_EXPIRED.reason());
        verify(cardManagementClient, never()).reserve(any(BigDecimal.class), anyString(), anyString());
    }

    @Test
    void authorizeDeclineWhenInsufficientFunds() {
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
                BigDecimal.valueOf(1000),
                activeCardResponse.issuerId(),
                activeCardResponse.createdAt());
        doReturn(lowBalanceCard).when(cardManagementClient).getCard(anyString());

        AuthorizationResponse response = authService.authorize(correctRequest, Instant.now());

        assertThat(response.responseCode()).isEqualTo(INSUFFICIENT_FUNDS.code());
        assertThat(response.declineReason()).isEqualTo(INSUFFICIENT_FUNDS.reason());
        verify(cardManagementClient, never()).reserve(any(BigDecimal.class), anyString(), anyString());
    }

    @Test
    void authorizeDeclineWhenReserveThrowsException() {
        doReturn(activeCardResponse).when(cardManagementClient).getCard(anyString());
        when(limitUsageRepository.upsertLimitUsage(anyString(), any(LocalDate.class), any(BigDecimal.class),
                any(BigDecimal.class), any(BigDecimal.class)))
                .thenReturn(1);
        doThrow(new ReserveException("Reserve failed")).when(cardManagementClient).reserve(any(BigDecimal.class), anyString(),
                anyString());

        AuthorizationResponse response = authService.authorize(correctRequest, Instant.now());

        assertThat(response.responseCode()).isEqualTo(RESERVATION_FAILED.code());
        assertThat(response.declineReason()).isEqualTo(RESERVATION_FAILED.reason());
        verify(cardManagementClient, times(1)).reserve(eq(correctRequest.amount()), anyString(), eq(correctRequest.pan()));
    }

    @Test
    void generateAuthCode_shouldReturnSixDigitAlphanumeric() {
        String code = authService.generateAuthCode();

        assertThat(code).hasSize(6);
        assertThat(code).matches("[0-9A-Z]{6}");
    }

    @Test
    void authorizeApprovedWhenDailyLimitNotReached() {
        doReturn(activeCardResponse).when(cardManagementClient).getCard(anyString());
        doNothing().when(cardManagementClient).reserve(any(BigDecimal.class), anyString(), anyString());

        when(limitUsageRepository.upsertLimitUsage(anyString(), any(LocalDate.class), any(BigDecimal.class),
                any(BigDecimal.class), any(BigDecimal.class)))
                .thenReturn(1);

        AuthorizationResponse response = authService.authorize(correctRequest, Instant.now());

        assertThat(response.status()).isEqualTo(AuthorizationResponse.STATUS_APPROVED);
        assertThat(response.responseCode()).isEqualTo(AuthorizationResponse.CODE_APPROVED);
        assertThat(response.rrn()).isNotNull();
        assertThat(response.authCode()).isNotNull();
        assertThat(response.declineReason()).isNull();
    }

    @Test
    void authorizeDeclineWhenDailyLimitReached() {
        doReturn(activeCardResponse).when(cardManagementClient).getCard(anyString());
        when(limitUsageRepository.upsertLimitUsage(anyString(), any(LocalDate.class), any(BigDecimal.class),
                any(BigDecimal.class), any(BigDecimal.class)))
                .thenReturn(0);

        AuthorizationResponse response = authService.authorize(correctRequest, Instant.now());

        assertThat(response.status()).isEqualTo(AuthorizationResponse.STATUS_DECLINED);
        assertThat(response.responseCode()).isEqualTo(EXCEEDS_AMOUNT_LIMIT.code());
        assertThat(response.declineReason()).isEqualTo(EXCEEDS_AMOUNT_LIMIT.reason());
        verify(cardManagementClient, never()).reserve(any(BigDecimal.class), anyString(), anyString());
    }

    @Test
    void authorizeDeclineWhenMonthlyLimitReached() {
        doReturn(activeCardResponse).when(cardManagementClient).getCard(anyString());

        when(limitUsageRepository.upsertLimitUsage(anyString(), any(LocalDate.class), any(BigDecimal.class),
                any(BigDecimal.class), any(BigDecimal.class)))
                .thenReturn(0);

        AuthorizationResponse response = authService.authorize(correctRequest, Instant.now());

        assertThat(response.status()).isEqualTo(AuthorizationResponse.STATUS_DECLINED);
        assertThat(response.responseCode()).isEqualTo(EXCEEDS_AMOUNT_LIMIT.code());
        assertThat(response.declineReason()).isEqualTo(EXCEEDS_AMOUNT_LIMIT.reason());
        verify(cardManagementClient, never()).reserve(any(BigDecimal.class), anyString(), anyString());
    }

    @Test
    void maskPAN_ShouldMaskMiddleEightCharacters() {
        String input = "4000001234560001";
        String result = MaskPan.maskPan(input);

        assertEquals("4000********0001", result);
        assertEquals(16, result.length());
    }

    @Test
    void maskPAN_ShouldReturnInvalidPanOnShortString() {
        String input = "4000";
        String result = MaskPan.maskPan(input);

        assertEquals("****", result);
    }

    @Test
    void maskPAN_ShouldReturnEmptyStringOnNull() {
        String input = null;
        String result = MaskPan.maskPan(input);

        assertEquals("", result);
    }

    @Test
    void maskPAN_ShouldPreserveOriginalLength() {
        String input1 = "4000001234560001";
        String result1 = MaskPan.maskPan(input1);

        String input2 = "400001";
        String result2 = MaskPan.maskPan(input2);

        assertEquals(input1.length(), result1.length());
        assertEquals(input2.length(), result2.length());
    }

    @Test
    void testAccumulatedRoundingError_withBigDecimalShouldBeExact() {
        BigDecimal limit = new BigDecimal("1000.00");
        BigDecimal amount = new BigDecimal("333.34");

        BigDecimal firstReserve = amount;
        BigDecimal secondReserve = amount;
        BigDecimal thirdReserve = amount;
        BigDecimal totalUsed = firstReserve.add(secondReserve).add(thirdReserve);

        boolean isExceeded = totalUsed.compareTo(limit) > 0;
        assertTrue(isExceeded, "Превышение лимита с типом long не было бы обнаружено");
    }

    @Test
    void rollbackReturnApprovedWhenCmsRollbackSucceeds() {
        doNothing().when(cardManagementClient).rollback(any(RollbackRequest.class));

        RollbackResponse response = authService.rollback(rollbackRequest, Instant.now());

        assertThat(response.status()).isEqualTo(RollbackResponse.STATUS_APPROVED);
        assertThat(response.rrn()).isEqualTo("123456789012");
        assertThat(response.declineReason()).isNull();
        verify(cardManagementClient, times(1)).rollback(rollbackRequest);
    }

    @Test
    void rollbackShouldReturnDeclinedWhenCmsReturnsInternalError() {
        doThrow(new InternalCardManagerException("Internal card management error"))
                .when(cardManagementClient).rollback(any(RollbackRequest.class));

        RollbackResponse response = authService.rollback(rollbackRequest, Instant.now());

        assertThat(response.status()).isEqualTo(RollbackResponse.STATUS_DECLINED);
        assertThat(response.declineReason()).isEqualTo(SERVICE_UNAVAILABLE.reason());
    }

    @Test
    void rollbackShouldReturnDeclinedWhenCardNotFound() {
        doThrow(new CardNotFoundException("Card not found"))
                .when(cardManagementClient).rollback(any(RollbackRequest.class));

        RollbackResponse response = authService.rollback(rollbackRequest, Instant.now());

        assertThat(response.status()).isEqualTo(RollbackResponse.STATUS_DECLINED);
        assertThat(response.declineReason()).isEqualTo(TRANSACTION_NOT_FOUND.reason());
    }

    @Test
    void rollbackShouldReturnDeclinedWhenAlreadyRolledBack() {
        doThrow(new RollbackConflictException("Rollback conflict"))
                .when(cardManagementClient).rollback(any(RollbackRequest.class));

        RollbackResponse response = authService.rollback(rollbackRequest, Instant.now());

        assertThat(response.status()).isEqualTo(RollbackResponse.STATUS_DECLINED);
        assertThat(response.declineReason()).isEqualTo(ALREADY_ROLLED_BACK.reason());
    }

    @Test
    void rollbackShouldReturnDeclinedWhenServiceUnavailable() {
        doThrow(new ServiceUnavailableException("Card Management service unavailable"))
                .when(cardManagementClient).rollback(any(RollbackRequest.class));

        RollbackResponse response = authService.rollback(rollbackRequest, Instant.now());

        assertThat(response.status()).isEqualTo(RollbackResponse.STATUS_DECLINED);
        assertThat(response.declineReason()).isEqualTo(SERVICE_UNAVAILABLE.reason());
    }

    @Test
    void rollbackShouldReturnDeclinedWhenInvalidRequest() {
        doThrow(new InvalidRollbackRequestException("Invalid rollback request"))
                .when(cardManagementClient).rollback(any(RollbackRequest.class));

        RollbackResponse response = authService.rollback(rollbackRequest, Instant.now());

        assertThat(response.status()).isEqualTo(RollbackResponse.STATUS_DECLINED);
        assertThat(response.declineReason()).isEqualTo(TRANSACTION_NOT_FOUND.reason());
    }

    @Test
    void rollbackShouldReturnDeclinedWhenUnexpectedError() {
        doThrow(new RuntimeException("Unexpected error"))
                .when(cardManagementClient).rollback(any(RollbackRequest.class));

        RollbackResponse response = authService.rollback(rollbackRequest, Instant.now());

        assertThat(response.status()).isEqualTo(RollbackResponse.STATUS_DECLINED);
        assertThat(response.declineReason()).isEqualTo(UNKNOWN_REASON.reason());
    }
}

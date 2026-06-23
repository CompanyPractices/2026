package com.processing.authorization.services;

import com.processing.authorization.client.CardManagementClient;
import com.processing.authorization.constants.DeclineOutcome;
import com.processing.authorization.events.*;
import com.processing.common.dto.authorization.AuthorizationRequest;
import com.processing.common.dto.authorization.AuthorizationResponse;
import com.processing.common.dto.authorization.RollbackRequest;
import com.processing.common.dto.authorization.RollbackResponse;
import com.processing.common.dto.cardmanagement.CardModel;
import com.processing.common.dto.cardmanagement.CardModelStatus;
import com.processing.authorization.exceptions.*;
import com.processing.authorization.repositories.LimitUsageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.stereotype.Service;

import java.time.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.format.DateTimeFormatter;
import java.util.Random;

import org.springframework.web.client.ResourceAccessException;

/**
 * Сервис авторизации транзакций по банковским картам.
 * <p>
 * Выполняет полный цикл авторизации карточной транзакции, включающий:
 * </p>
 * <ol>
 * <li>Получение данных карты из Card Management System (CMS)</li>
 * <li>Проверку статуса карты (ACTIVE, BLOCKED, INACTIVE, EXPIRED)</li>
 * <li>Проверку срока действия карты</li>
 * <li>Проверку доступного баланса</li>
 * <li>Резервирование средств на карте</li>
 * <li>Генерацию уникальных идентификаторов транзакции (RRN и AuthCode)</li>
 * </ol>
 * <p>
 * В случае любой ошибки на любом из этапов возвращается declined-ответ
 * с соответствующим кодом причины отклонения.
 * </p>
 *
 * @author core-auth-team
 * @see AuthorizationRequest
 * @see AuthorizationResponse
 * @see CardModel
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    private final LimitUsageRepository limitUsageRepository;
    private final AuthorizationEventNotifier eventNotifier;

    private final CardManagementClient cardManagementClient;

    /**
     * Выполняет авторизацию транзакции по банковской карте.
     *
     * @param request запрос на авторизацию, содержащий PAN карты, сумму и другие
     *                параметры
     * @return {@link AuthorizationResponse} с результатом авторизации:
     *         <ul>
     *         <li>При успехе: статус "approved", RRN и AuthCode</li>
     *         <li>При отказе: статус "declined", причина отказа и код ответа</li>
     *         </ul>
     *
     * @see #generateRRN(AuthorizationRequest)
     * @see #generateAuthCode()
     * @see AuthorizationRequest
     * @see AuthorizationResponse
     */
    @Transactional(rollbackFor = { Exception.class })
    public AuthorizationResponse authorize(AuthorizationRequest request, Instant requestInputTime) {
        CardModel cardResponse;
        try {
            cardResponse = cardManagementClient.getCard(request.pan());
        } catch (CardNotFoundException | InvalidGetCardRequestException e) {
            eventNotifier.notify(new AuthServiceAuthDeclineNoCardEvent(request.pan()));
            return DeclineOutcome.CARD_NOT_FOUND.buildAuthorization(request, requestInputTime);
        } catch (ServiceUnavailableException | ResourceAccessException | InternalCardManagerException e) {
            eventNotifier.notify(new AuthServiceAuthDeclineNoServiceEvent(request.pan(), "cmsUrl", e.getMessage()));
            return DeclineOutcome.SERVICE_UNAVAILABLE.buildAuthorization(request, requestInputTime);
        } catch (PaymentRequiredException e) {
            eventNotifier.notify(new AuthServiceAuthDeclinedCardStatusEvent(request.pan(),
                    "Payment required from Card Management Service"));
            return DeclineOutcome.CARD_BLOCKED.buildAuthorization(request, requestInputTime);
        } catch (GetCardException e) {
            eventNotifier.notify(new AuthServiceAuthDeclineNoServiceEvent(request.pan(), "cmsUrl", e.getMessage()));
            return DeclineOutcome.SERVICE_UNAVAILABLE.buildAuthorization(request, requestInputTime);
        } catch (Exception e) {
            eventNotifier.notify(new AuthServiceAuthDeclineUnknownEvent(request.pan(), e.getMessage()));
            return DeclineOutcome.UNKNOWN_REASON.buildAuthorization(request, requestInputTime);
        }

        CardModelStatus currCardStatus = cardResponse.status();
        if (currCardStatus == null) {
            eventNotifier.notify(new AuthServiceAuthDeclineUnknownEvent(request.pan(), "recieved null card status"));
            return DeclineOutcome.UNKNOWN_REASON.buildAuthorization(request, requestInputTime);
        }
        if (!currCardStatus.equals(CardModelStatus.ACTIVE)) {
            return switch (currCardStatus) {
                case CardModelStatus.EXPIRED -> {
                    eventNotifier
                            .notify(new AuthServiceAuthDeclinedCardStatusEvent(request.pan(),
                                    currCardStatus.toString()));
                    yield DeclineOutcome.CARD_EXPIRED.buildAuthorization(request, requestInputTime);
                }
                case CardModelStatus.BLOCKED -> {
                    eventNotifier
                            .notify(new AuthServiceAuthDeclinedCardStatusEvent(request.pan(),
                                    currCardStatus.toString()));
                    yield DeclineOutcome.CARD_BLOCKED.buildAuthorization(request, requestInputTime);
                }
                case CardModelStatus.INACTIVE -> {
                    eventNotifier
                            .notify(new AuthServiceAuthDeclinedCardStatusEvent(request.pan(),
                                    currCardStatus.toString()));
                    yield DeclineOutcome.CARD_INACTIVE.buildAuthorization(request, requestInputTime);
                }
                default -> {
                    eventNotifier
                            .notify(new AuthServiceAuthDeclineUnknownEvent(request.pan(), "recieved null card status"));
                    yield DeclineOutcome.UNKNOWN_REASON.buildAuthorization(request, requestInputTime);
                }
            };
        }

        LocalDate transmissionDate = request.transmissionDateTime().atZone(ZoneOffset.UTC).toLocalDate();

        LocalDate lastValidDay = cardResponse.expiryDate().atEndOfMonth();
        if (lastValidDay.isBefore(transmissionDate)) {
            eventNotifier
                    .notify(new AuthServiceAuthDeclinedCardExpiryDateEvent(request.pan(), lastValidDay.toString()));
            return DeclineOutcome.CARD_EXPIRED.buildAuthorization(request, requestInputTime);
        }

        if (request.amount().compareTo(cardResponse.availableBalance()) > 0) {
            eventNotifier.notify(new AuthServiceAuthDeclinedFundsEvent(request.pan()));
            return DeclineOutcome.INSUFFICIENT_FUNDS.buildAuthorization(request, requestInputTime);
        }

        LocalDate transmissionLocalDate = request.transmissionDateTime()
                .atZone(ZoneOffset.UTC) // TODO: msc
                .toLocalDate();

        boolean areLimitsUpdated = false;
        try {
            areLimitsUpdated = checkAndUpdateLimits(cardResponse, request.amount(), transmissionLocalDate);
        } catch (DuplicateKeyException | ConstraintViolationException e) {
            eventNotifier.notify(new AuthServiceDuplicateKeyEvent(request.pan(), e.getMessage()));
            try {
                areLimitsUpdated = checkAndUpdateLimits(cardResponse, request.amount(), transmissionLocalDate);
            } catch (Exception ex) {
                eventNotifier
                        .notify(new AuthServiceAuthDeclineNoServiceEvent(
                                request.pan(),
                                "limit_usage table",
                                ex.getMessage()));
                return DeclineOutcome.DB_UNAVAILABLE.buildAuthorization(request, requestInputTime);
            }
        } catch (Exception e) {
            eventNotifier.notify(
                    new AuthServiceAuthDeclineNoServiceEvent(request.pan(), "limit_usage table", e.getMessage()));
            return DeclineOutcome.DB_UNAVAILABLE.buildAuthorization(request, requestInputTime);
        }

        if (!areLimitsUpdated) {
            eventNotifier.notify(new AuthServiceAuthDeclinedLimitsEvent(request.pan()));
            return DeclineOutcome.EXCEEDS_AMOUNT_LIMIT.buildAuthorization(request, requestInputTime);
        }

        String rrn = generateRRN(request);
        try {
            cardManagementClient.reserve(request.amount(), rrn, request.pan());
        } catch (CardNotFoundException e) {
            eventNotifier.notify(new AuthServiceAuthDeclineNoCardEvent(request.pan()));
            return DeclineOutcome.CARD_NOT_FOUND.buildAuthorization(request, requestInputTime);
        } catch (ServiceUnavailableException | ResourceAccessException | InternalCardManagerException e) {
            eventNotifier.notify(new AuthServiceAuthDeclineNoServiceEvent(request.pan(), "cmsUrl", e.getMessage()));
            return DeclineOutcome.SERVICE_UNAVAILABLE.buildAuthorization(request, requestInputTime);
        } catch (InvalidReserveRequestException e) {
            eventNotifier.notify(new AuthServiceAuthDeclineNoCardEvent(request.pan()));
            return DeclineOutcome.CARD_NOT_FOUND.buildAuthorization(request, requestInputTime);
        } catch (InsufficientFundsException e) {
            eventNotifier.notify(new AuthServiceAuthDeclinedFundsEvent(request.pan()));
            return DeclineOutcome.INSUFFICIENT_FUNDS.buildAuthorization(request, requestInputTime);
        } catch (ReserveException e) {
            eventNotifier.notify(new AuthServiceAuthDeclineNoServiceEvent(request.pan(), "cmsUrl", e.getMessage()));
            return DeclineOutcome.RESERVATION_FAILED.buildAuthorization(request, requestInputTime);
        } catch (Exception e) {
            eventNotifier.notify(new AuthServiceAuthDeclineUnknownEvent(request.pan(), e.getMessage()));
            return DeclineOutcome.UNKNOWN_REASON.buildAuthorization(request, requestInputTime);
        }

        String authCode = generateAuthCode();

        eventNotifier.notify(new AuthServiceAuthorizeEvent(request.pan()));
        return AuthorizationResponse.approved(request, rrn, authCode, requestInputTime);
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean checkAndUpdateLimits(CardModel cardResponse, BigDecimal amount, LocalDate transmissionLocalDate) {
        int updatedCount = limitUsageRepository.upsertLimitUsage(cardResponse.pan(), transmissionLocalDate,
                amount, cardResponse.dailyLimit(), cardResponse.monthlyLimit());
        if (updatedCount == 0) {
            return false;
        }
        return true;
    }

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyDDD");

    public String generateRRN(AuthorizationRequest request) {
        long nextRrn = limitUsageRepository.getNextRrn();
        limitUsageRepository.saveRrn(nextRrn);
        String currentDay = FORMATTER.format(LocalDateTime.now()).substring(1);

        String rrn = currentDay + String.format("%08d", nextRrn);
        log.info("Generated RRN for STAN {}: {}",
                request.stan(), rrn);
        return rrn;
    }

    private static final Random RANDOM = new SecureRandom();

    private static final byte[] ALPHABET = {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B',
            'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N',
            'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
    };

    /**
     * Генерирует случайный код авторизации (AuthCode) для транзакции.
     *
     * <p>
     * AuthCode представляет собой 6-символьную строку, состоящую из
     * случайных букв верхнего регистра (A-Z) и цифр (0-9). Используется
     * как дополнительный идентификатор одобренной транзакции.
     * </p>
     *
     * <p>
     * <b>Пример AuthCode:</b> "A3F9K2", "Z7M1X0"
     * </p>
     *
     * <p>
     * Алфавит генерации: 36 символов (0-9, A-Z)
     * </p>
     *
     * @return строка из 6 случайных символов (буквы и цифры)
     */
    public String generateAuthCode() {
        byte[] buf = new byte[6];
        for (int i = 0; i < buf.length; i++) {
            buf[i] = ALPHABET[RANDOM.nextInt(ALPHABET.length)];
        }
        return new String(buf, StandardCharsets.US_ASCII);
    }

    public RollbackResponse rollback(RollbackRequest request, Instant requestInputTime) {
        try {
            cardManagementClient.rollback(request);
        } catch (CardNotFoundException | InvalidRollbackRequestException e) {
            eventNotifier.notify(new AuthServiceRollbackDeclineNoCardEvent(request.pan()));
            return DeclineOutcome.TRANSACTION_NOT_FOUND.buildRollback(request, requestInputTime);
        } catch (ServiceUnavailableException | ResourceAccessException | InternalCardManagerException e) {
            eventNotifier.notify(new AuthServiceRollbackDeclineNoServiceEvent(request.pan(), "cmsUrl", e.getMessage()));
            return DeclineOutcome.SERVICE_UNAVAILABLE.buildRollback(request, requestInputTime);
        } catch (RollbackConflictException e) {
            eventNotifier.notify(new AuthServiceRollbackDeclineConflictEvent(request.pan()));
            return DeclineOutcome.ALREADY_ROLLED_BACK.buildRollback(request, requestInputTime);
        } catch (RollbackFailureException e) {
            eventNotifier.notify(new AuthServiceRollbackDeclineNoServiceEvent(request.pan(), "cmsUrl", e.getMessage()));
            return DeclineOutcome.ROLLBACK_FAILED.buildRollback(request, requestInputTime);
        } catch (Exception e) {
            eventNotifier.notify(new AuthServiceRollbackDeclineUnknownEvent(request.pan(), e.getMessage()));
            return DeclineOutcome.UNKNOWN_REASON.buildRollback(request, requestInputTime);
        }

        eventNotifier.notify(new AuthServiceRollbackEvent(request.pan()));
        return RollbackResponse.approved(request, requestInputTime);

    }
}

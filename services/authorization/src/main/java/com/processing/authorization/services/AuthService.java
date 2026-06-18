package com.processing.authorization.services;

import com.processing.authorization.constants.DeclineOutcome;
import com.processing.common.dto.authorization.AuthorizationRequest;
import com.processing.common.dto.authorization.AuthorizationResponse;
import com.processing.common.dto.authorization.RollbackRequest;
import com.processing.common.dto.authorization.RollbackResponse;
import com.processing.common.dto.cardmanagement.CardModel;
import com.processing.authorization.entities.LimitUsage;
import com.processing.common.dto.cardmanagement.CardModelStatus;
import com.processing.authorization.exceptions.*;
import com.processing.common.dto.cardmanagement.ReserveRequest;
import com.processing.common.utils.MaskPan;
import com.processing.authorization.repositories.LimitUsageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.*;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

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
    private final RestClient restClient;

    /**
     * Базовый URL Card Management System.
     * Загружается из конфигурации {@code card-management.url}.
     */
    @Value("${card-management.url}")
    private String cmsUrl;

    private final LimitUsageRepository limitUsageRepository;

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
     * @see #getCard(String)
     * @see #reserve(BigDecimal, String, String)
     * @see #generateRRN()
     * @see #generateAuthCode()
     * @see AuthorizationRequest
     * @see AuthorizationResponse
     */
    @Transactional(rollbackFor = { Exception.class })
    public AuthorizationResponse authorize(AuthorizationRequest request, Instant requestInputTime) {
        CardModel cardResponse;
        try {
            cardResponse = getCard(request.pan());
        } catch (CardNotFoundException e) {
            log.error("card not found for pan: {}", logPan(request.pan()), e);
            return DeclineOutcome.CARD_NOT_FOUND.buildAuthorization(request, requestInputTime);
        } catch (ServiceUnavailableException | ResourceAccessException | InternalCardManagerException e) {
            log.error("service unavailable for pan: {}", logPan(request.pan()), e);
            return DeclineOutcome.SERVICE_UNAVAILABLE.buildAuthorization(request, requestInputTime);
        } catch (InvalidGetCardRequestException e) {
            log.error("card not found for pan: {}", logPan(request.pan()), e);
            return DeclineOutcome.CARD_NOT_FOUND.buildAuthorization(request, requestInputTime);
        } catch (PaymentRequiredException e) {
            log.error("card required payment for pan: {}", logPan(request.pan()), e);
            return DeclineOutcome.CARD_BLOCKED.buildAuthorization(request, requestInputTime);
        } catch (GetCardException e) {
            log.error("get card from card-management service failed for pan: {}", logPan(request.pan()), e);
            return DeclineOutcome.CARD_BLOCKED.buildAuthorization(request, requestInputTime);
        } catch (Exception e) {
            log.error("getting card failed for pan: {}", logPan(request.pan()), e);
            return DeclineOutcome.UNKNOWN_REASON.buildAuthorization(request, requestInputTime);
        }

        CardModelStatus currCardStatus = cardResponse.status();
        if (currCardStatus == null) {
            return DeclineOutcome.UNKNOWN_REASON.buildAuthorization(request, requestInputTime);
        }
        if (!currCardStatus.equals(CardModelStatus.ACTIVE)) {
            return switch (currCardStatus) {
                case CardModelStatus.EXPIRED ->
                    DeclineOutcome.CARD_EXPIRED.buildAuthorization(request, requestInputTime);
                case CardModelStatus.BLOCKED ->
                    DeclineOutcome.CARD_BLOCKED.buildAuthorization(request, requestInputTime);
                case CardModelStatus.INACTIVE ->
                    DeclineOutcome.CARD_INACTIVE.buildAuthorization(request, requestInputTime);
                default -> DeclineOutcome.UNKNOWN_REASON.buildAuthorization(request, requestInputTime);
            };
        }

        LocalDate transmissionDate = request.transmissionDateTime().atZone(ZoneOffset.UTC).toLocalDate();

        LocalDate lastValidDay = cardResponse.expiryDate().atEndOfMonth();
        if (lastValidDay.isBefore(transmissionDate)) {
            return DeclineOutcome.CARD_EXPIRED.buildAuthorization(request, requestInputTime);
        }

        if (request.amount().compareTo(cardResponse.availableBalance()) > 0) {
            return DeclineOutcome.INSUFFICIENT_FUNDS.buildAuthorization(request, requestInputTime);
        }

        LocalDate transmissionLocalDate = request.transmissionDateTime()
                .atZone(ZoneOffset.UTC)
                .toLocalDate();

        if (checkAndUpdateLimits(request, cardResponse, transmissionLocalDate)) {
            return DeclineOutcome.EXCEEDS_AMOUNT_LIMIT.buildAuthorization(request, requestInputTime);
        }

        String rrn = generateRRN();
        try {
            reserve(request.amount(), rrn, request.pan());
        } catch (CardNotFoundException e) {
            log.error("card not found for pan: {}", logPan(request.pan()), e);
            return DeclineOutcome.CARD_NOT_FOUND.buildAuthorization(request, requestInputTime);
        } catch (ServiceUnavailableException | ResourceAccessException | InternalCardManagerException e) {
            log.error("service unavailable for pan: {}", logPan(request.pan()), e);
            return DeclineOutcome.SERVICE_UNAVAILABLE.buildAuthorization(request, requestInputTime);
        } catch (InvalidReserveRequestException e) {
            log.error("invalid reverse request for pan: {}", logPan(request.pan()), e);
            return DeclineOutcome.CARD_NOT_FOUND.buildAuthorization(request, requestInputTime);
        } catch (InsufficientFundsException e) {
            log.error("Insufficient funds from card-management for pan: {}", logPan(request.pan()), e);
            return DeclineOutcome.INSUFFICIENT_FUNDS.buildAuthorization(request, requestInputTime);
        } catch (ReserveException e) {
            log.error("reserve from card-management service failed for pan: {}", logPan(request.pan()), e);
            return DeclineOutcome.RESERVATION_FAILED.buildAuthorization(request, requestInputTime);
        } catch (Exception e) {
            log.error("reserve failed for card {}", cardResponse.id(), e);
            return DeclineOutcome.RESERVATION_FAILED.buildAuthorization(request, requestInputTime);
        }

        String authCode = generateAuthCode();
        return AuthorizationResponse.approved(request, rrn, authCode, requestInputTime);
    }

    /**
     * Получает информацию о карте из Card Management System по номеру PAN.
     *
     * <p>
     * Выполняет GET-запрос к CMS на эндпоинт {@code /api/cards/{pan}}.
     * Обрабатывает различные HTTP-статусы ответа:
     * </p>
     * <ul>
     * <li><b>404 Not Found</b> - карта не найдена, выбрасывает
     * {@link CardNotFoundException}</li>
     * <li><b>503 Service Unavailable</b> - CMS недоступен, выбрасывает
     * {@link ServiceUnavailableException}</li>
     * <li><b>Другие ошибки (не 2xx)</b> - общая ошибка получения карты</li>
     * <li><b>2xx Success</b> - возвращает объект {@link CardModel} с данными
     * карты</li>
     * </ul>
     *
     * @param pan номер карты (Primary Account Number) - 16-значный номер
     * @return {@link CardModel} объект с полной информацией о карте:
     *         статус, срок действия, доступный баланс и другие атрибуты
     *
     * @see CardModel
     * @see CardNotFoundException
     * @see ServiceUnavailableException
     */
    public CardModel getCard(String pan) {
        URI uri = UriComponentsBuilder
                .fromUriString(cmsUrl)
                .scheme("http")
                .path("/api/cards/{pan}")
                .buildAndExpand(pan)
                .toUri();
        log.debug("Getting card info for pan {}", logPan(pan));

        return restClient.get()
                .uri(uri)
                .retrieve()
                .onStatus(status -> status.value() == 500, (req, res) -> {
                    throw new InternalCardManagerException("Internal card management error");
                })
                .onStatus(status -> status.value() == 503, (req, res) -> {
                    throw new ServiceUnavailableException("Card Management service unavailable");
                })
                .onStatus(status -> status.value() == 400, (req, res) -> {
                    throw new InvalidGetCardRequestException("Invalid pan");
                })
                .onStatus(status -> status.value() == 402, (req, res) -> {
                    throw new PaymentRequiredException("Payment Required from card-management");
                })
                .onStatus(status -> status.value() == 404, (req, res) -> {
                    throw new CardNotFoundException("Card not found");
                })
                .onStatus(status -> !status.is2xxSuccessful(), (req, res) -> {
                    throw new GetCardException("Failed to get card. Status: " + res.getStatusCode());
                })
                .body(CardModel.class);
    }

    /**
     * Резервирует указанную сумму на карте через Card Management System.
     *
     * <p>
     * Выполняет POST-запрос к CMS на эндпоинт {@code /api/cards/{pan}/reserve}
     * с телом запроса, содержащим сумму резервирования и RRN транзакции.
     * Резервирование необходимо для блокировки средств на карте до момента
     * фактического списания.
     * </p>
     *
     * <p>
     * В случае ошибки резервирования (не 2xx статус) выбрасывается
     * {@link ReserveException}.
     * </p>
     *
     * @param amount сумма для резервирования в минимальных единицах валюты
     *               (копейки, центы)
     * @param rrn    уникальный идентификатор транзакции (Retrieval Reference
     *               Number)
     * @param pan    номер карты для резервирования средств
     * @see ReserveRequest
     * @see ReserveException
     */
    public void reserve(BigDecimal amount, String rrn, String pan) {
        ReserveRequest reserveRequest = new ReserveRequest(amount, rrn);
        URI uri = UriComponentsBuilder
                .fromUriString(cmsUrl)
                .scheme("http")
                .path("/api/cards/{pan}/reserve")
                .buildAndExpand(pan)
                .toUri();
        log.debug("Reserving amount {} for card {} with rrn {}", amount, logPan(pan), rrn);
        restClient.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .body(reserveRequest)
                .retrieve()
                .onStatus(status -> status.value() == 500, (req, res) -> {
                    throw new InternalCardManagerException("Internal card management error");
                })
                .onStatus(status -> status.value() == 503, (req, res) -> {
                    throw new ServiceUnavailableException("Card Management service unavailable");
                })
                .onStatus(status -> status.value() == 400, (req, res) -> {
                    throw new InvalidReserveRequestException("Invalid reserve request");
                })
                .onStatus(status -> status.value() == 402, (req, res) -> {
                    throw new InsufficientFundsException("Insufficient Funds from card-management");
                })
                .onStatus(status -> status.value() == 404, (req, res) -> {
                    throw new CardNotFoundException("Card not found");
                })
                .onStatus(status -> !status.is2xxSuccessful(), (req, res) -> {
                    throw new ReserveException("Failed to reserve. Status: " + res.getStatusCode());
                })
                .toBodilessEntity();

        log.debug("Reserve successful for card {}", logPan(pan));
    }

    // TODO change transmissionDate to Instant type
    // public void updateLimits(AuthorizationRequest request, Instant transmissionDate,
    //         Optional<LimitUsage> currLimitUsage, Optional<LimitUsage> monthLimitUsage) {
    //     if (currLimitUsage.isPresent()) {
    //         LimitUsage usage = currLimitUsage.get();
    //         usage.setMonthlyAmount(usage.getMonthlyAmount().add(request.amount()));
    //         usage.setDailyAmount(usage.getDailyAmount().add(request.amount()));
    //         limitUsageRepository.save(usage);
    //     } else if (monthLimitUsage.isPresent()) {
    //         LimitUsage monthUsage = monthLimitUsage.get();
    //         LimitUsage usage = new LimitUsage();
    //         usage.setPan(request.pan());
    //         usage.setUsageDate(transmissionDate);
    //         usage.setDailyAmount(request.amount());
    //         usage.setMonthlyAmount(monthUsage.getMonthlyAmount().add(request.amount()));
    //         limitUsageRepository.save(usage);
    //     } else {
    //         LimitUsage usage = new LimitUsage();
    //         usage.setPan(request.pan());
    //         usage.setUsageDate(transmissionDate);
    //         usage.setDailyAmount(request.amount());
    //         usage.setMonthlyAmount(request.amount());
    //         limitUsageRepository.save(usage);
    //     }
    // }

    // @Transactional(rollbackFor = Exception.class)
    public boolean checkAndUpdateLimits(AuthorizationRequest request, CardModel cardResponse,
            LocalDate transmissionLocalDate) {
        Optional<LimitUsage> currLimitUsage = limitUsageRepository
                .findByPanAndUsageDate(request.pan(), transmissionLocalDate);

        Optional<LimitUsage> monthLimitUsage = currLimitUsage.isPresent()
                ? currLimitUsage
                : limitUsageRepository
                        .findTopByPanAndUsageDateBetweenOrderByUsageDateDesc(
                                request.pan(),
                                transmissionLocalDate.withDayOfMonth(1)
                                        .atStartOfDay()
                                        .toLocalDate(),
                                transmissionLocalDate);

        if (monthLimitUsage.isPresent()) {
            LimitUsage monthUsage = monthLimitUsage.get();
            if (monthUsage.getMonthlyAmount().add(request.amount()).compareTo(cardResponse.monthlyLimit()) > 0) {
                return true;
            }
        } else if (request.amount().compareTo(cardResponse.monthlyLimit()) > 0) {
            return true;
        }

        if (currLimitUsage.isPresent()) {
            LimitUsage usage = currLimitUsage.get();
            if (usage.getDailyAmount().add(request.amount()).compareTo(cardResponse.dailyLimit()) > 0) {
                return true;
            }
        } else if (request.amount().compareTo(cardResponse.dailyLimit()) > 0) {
            return true;
        }
        return false;
    }

    private final AtomicReference<String> lastTimestampAndSeq = new AtomicReference<>("");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyDDDHHmmss");

    /**
     * Генерирует уникальный Retrieval Reference Number (RRN) для транзакции.
     *
     * <p>
     * Формат RRN: 10 цифр
     * </p>
     * <ul>
     * <li>Позиция 1: последняя цифра года (0-9)</li>
     * <li>Позиции 2-4: день года (001-366)</li>
     * <li>Позиции 5-6: час (00-23)</li>
     * <li>Позиции 7-8: минуты (00-59)</li>
     * <li>Позиции 9-10: секунды (00-59) + порядковый номер (00-99)</li>
     * </ul>
     *
     * <p>
     * Для обеспечения уникальности при генерации нескольких RRN в одну секунду
     * используется атомарный счетчик (CAS-операция). Если за секунду генерируется
     * более 100 RRN, счетчик сбрасывается и начинается заново.
     * </p>
     *
     * <p>
     * <b>Пример RRN:</b> 3065121534 (2023 год, 65-й день, 12:15:34)
     * </p>
     *
     * @return строка из 10 цифр, представляющая уникальный RRN транзакции
     *
     * @see #lastTimestampAndSeq
     */
    public String generateRRN() {
        String currentSecond = FORMATTER.format(LocalDateTime.now()).substring(1);
        String nextValue;
        while (true) {
            String currentState = lastTimestampAndSeq.get();
            int nextSeq = 0;
            if (currentState != null && currentState.startsWith(currentSecond)) {
                int lastSeq = Integer.parseInt(currentState.substring(10));
                nextSeq = (lastSeq + 1) % 100;
            }

            nextValue = currentSecond + String.format("%02d", nextSeq);
            if (lastTimestampAndSeq.compareAndSet(currentState, nextValue)) {
                break;
            }
        }
        return nextValue;
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
     *
     * @see Random#ints(int, int, int)
     */
    public String generateAuthCode() {
        byte[] buf = new byte[6];
        for (int i = 0; i < buf.length; i++) {
            buf[i] = ALPHABET[RANDOM.nextInt(ALPHABET.length)];
        }
        return new String(buf, StandardCharsets.US_ASCII);
    }

    public String logPan(String pan) {
        return MaskPan.maskPan(pan);
    }

    public RollbackResponse rollback(RollbackRequest request, Instant requestInputTime) {
        try {
            rollbackCard(request);
        } catch (CardNotFoundException e) {
            log.error("card not found for pan: {}", logPan(request.pan()), e);
            return DeclineOutcome.TRANSACTION_NOT_FOUND.buildRollback(request, requestInputTime);
        } catch (ServiceUnavailableException | ResourceAccessException | InternalCardManagerException e) {
            log.error("service unavailable for pan: {}", logPan(request.pan()), e);
            return DeclineOutcome.SERVICE_UNAVAILABLE.buildRollback(request, requestInputTime);
        } catch (InvalidRollbackRequestException e) {
            log.error("invalid rollback request for pan: {}", logPan(request.pan()), e);
            return DeclineOutcome.TRANSACTION_NOT_FOUND.buildRollback(request, requestInputTime);
        } catch (RollbackConflictException e) {
            log.error("rollback request already completed for pan: {}", logPan(request.pan()), e);
            return DeclineOutcome.ALREADY_ROLLED_BACK.buildRollback(request, requestInputTime);
        } catch (RollbackFailureException e) {
            log.error("rollback from card-management service failed for pan: {}", logPan(request.pan()), e);
            return DeclineOutcome.ROLLBACK_FAILED.buildRollback(request, requestInputTime);
        } catch (Exception e) {
            log.error("rollback failed for pan: {}", logPan(request.pan()), e);
            return DeclineOutcome.UNKNOWN_REASON.buildRollback(request, requestInputTime);
        }
        return RollbackResponse.approved(request, requestInputTime);

    }

    public void rollbackCard(RollbackRequest request) {
        URI uri = UriComponentsBuilder
                .fromUriString(cmsUrl)
                .scheme("http")
                .path("/api/cards/{pan}/rollback")
                .buildAndExpand(request.pan())
                .toUri();
        log.debug("Rollback amount {} for card {} with rrn {}", request.amount(), logPan(request.pan()), request.rrn());
        restClient.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .onStatus(status -> status.value() == 500, (req, res) -> {
                    throw new InternalCardManagerException("Internal card management error");
                })
                .onStatus(status -> status.value() == 503, (req, res) -> {
                    throw new ServiceUnavailableException("Card Management service unavailable");
                })
                .onStatus(status -> status.value() == 400, (req, res) -> {
                    throw new InvalidRollbackRequestException("Invalid rollback request");
                })
                .onStatus(status -> status.value() == 404, (req, res) -> {
                    throw new CardNotFoundException("Card not found: " + logPan(request.pan()));
                })
                .onStatus(status -> status.value() == 409, (req, res) -> {
                    throw new RollbackConflictException("Rollback conflict");
                })
                .onStatus(status -> !status.is2xxSuccessful(), (req, res) -> {
                    throw new RollbackFailureException("Failed to rollback. Status: " + res.getStatusCode());
                })
                .toBodilessEntity();

        log.debug("Rollback successful for card {}", logPan(request.pan()));
    }
}

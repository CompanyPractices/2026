package com.processing.authorization.services;

import com.processing.authorization.constants.DeclineOutcome;
import com.processing.common.dto.authorization.AuthorizationRequest;
import com.processing.common.dto.authorization.AuthorizationResponse;
import com.processing.common.dto.cardmanagement.CardModel;
import com.processing.authorization.entities.LimitUsage;
import com.processing.common.dto.cardmanagement.CardModelStatus;
import com.processing.authorization.exceptions.CardNotFoundException;
import com.processing.authorization.exceptions.ReserveCardException;
import com.processing.authorization.exceptions.ServiceUnavailableException;
import com.processing.common.dto.cardmanagement.ReserveRequest;

import com.processing.authorization.repositories.LimitUsageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Calendar;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

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
    private final WebClient webClient;

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
     * @see #reserve(long, String, String)
     * @see #generateRRN()
     * @see #generateAuthCode()
     * @see AuthorizationRequest
     * @see AuthorizationResponse
     */
    @Transactional(rollbackFor = { Exception.class })
    public AuthorizationResponse authorize(AuthorizationRequest request, LocalDateTime requestInputTime) {
        CardModel cardResponse;
        try {
            cardResponse = getCard(request.pan());
        } catch (CardNotFoundException e) {
            log.error("card not found for pan: {}", maskPAN(request.pan()), e);
            return DeclineOutcome.CARD_NOT_FOUND.build(request, requestInputTime);
        } catch (ServiceUnavailableException | WebClientResponseException e) {
            log.error("service unavailable for pan: {}", maskPAN(request.pan()), e);
            return DeclineOutcome.SERVICE_UNAVAILABLE.build(request, requestInputTime);
        } catch (Exception e) {
            log.error("getting card from card management service failed for pan: {}", maskPAN(request.pan()), e);
            return DeclineOutcome.UNKNOWN_REASON.build(request, requestInputTime);
        }

        CardModelStatus currCardStatus = cardResponse.status();
        if (currCardStatus == null) {
            return DeclineOutcome.UNKNOWN_REASON.build(request, requestInputTime);
        }
        if (!currCardStatus.equals(CardModelStatus.ACTIVE)) {
            return switch (currCardStatus) {
                case CardModelStatus.EXPIRED -> DeclineOutcome.CARD_EXPIRED.build(request, requestInputTime);
                case CardModelStatus.BLOCKED -> DeclineOutcome.CARD_BLOCKED.build(request, requestInputTime);
                case CardModelStatus.INACTIVE -> DeclineOutcome.CARD_INACTIVE.build(request, requestInputTime);
                default -> DeclineOutcome.UNKNOWN_REASON.build(request, requestInputTime);
            };
        }

        LocalDate transmissionDate;
        if (request.transmissionDateTime().endsWith("Z")) {
            transmissionDate = OffsetDateTime.parse(request.transmissionDateTime()).toLocalDate();
        } else {
            transmissionDate = LocalDateTime.parse(request.transmissionDateTime()).toLocalDate();
        }

        LocalDate lastValidDay = cardResponse.expiryDate().atEndOfMonth();
        if (lastValidDay.isBefore(transmissionDate)) {
            return DeclineOutcome.CARD_EXPIRED.build(request, requestInputTime);
        }

        if (request.amount() > cardResponse.availableBalance()) {
            return DeclineOutcome.INSUFFICIENT_FUNDS.build(request, requestInputTime);
        }

        Optional<LimitUsage> currLimitUsage = limitUsageRepository
                .findByPanAndUsageDate(request.pan(), transmissionDate);

        if (currLimitUsage.isPresent()) {
            LimitUsage usage = currLimitUsage.get();
            if (usage.getDailyAmount() + request.amount() > cardResponse.dailyLimit()) {
                return DeclineOutcome.EXCEEDS_AMOUNT_LIMIT.build(request, requestInputTime);
            }
        } else if (request.amount() > cardResponse.dailyLimit()) {
            return DeclineOutcome.EXCEEDS_AMOUNT_LIMIT.build(request, requestInputTime);
        }

        Long monthlyLimitUsage = limitUsageRepository
                .sumMonthlyAmountByPanAndMonth(request.pan(), transmissionDate.withDayOfMonth(1), transmissionDate);
        if (monthlyLimitUsage + request.amount() > cardResponse.monthlyLimit()) {
            return DeclineOutcome.EXCEEDS_AMOUNT_LIMIT.build(request, requestInputTime);
        }

        String rrn = generateRRN();
        try {
            reserve(request.amount(), rrn, request.pan());
            if (currLimitUsage.isPresent()) {
                LimitUsage usage = currLimitUsage.get();
                usage.setMonthlyAmount(usage.getMonthlyAmount() + request.amount());
                usage.setDailyAmount(usage.getDailyAmount() + request.amount());
                limitUsageRepository.save(usage);
            } else {
                LimitUsage usage = new LimitUsage();
                usage.setPan(request.pan());
                usage.setUsageDate(transmissionDate);
                usage.setDailyAmount(request.amount());
                usage.setMonthlyAmount(monthlyLimitUsage + request.amount());
                limitUsageRepository.save(usage);
            }
        } catch (Exception e) {
            log.error("reserving failed for card {}", cardResponse.id(), e);
            return DeclineOutcome.RESERVATION_FAILED.build(request, requestInputTime);
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
     * @throws Exception если карта не найдена, сервис недоступен или произошла
     *                   другая ошибка.
     *                   Конкретный тип исключения можно получить через
     *                   {@link Exception#getCause()}:
     *                   {@link CardNotFoundException} или
     *                   {@link ServiceUnavailableException}
     *
     * @see CardModel
     * @see CardNotFoundException
     * @see ServiceUnavailableException
     */
    public CardModel getCard(String pan) throws Exception {
        String fullUrl = cmsUrl.startsWith("http") ? cmsUrl : "http://" + cmsUrl;
        String getCardhUrl = fullUrl + "/api/cards";
        String url = getCardhUrl + "/" + pan;
        log.debug("Getting card info for pan {}", maskPAN(pan));

        CardModel response = webClient.get()
                .uri(url)
                .retrieve()
                .onStatus(status -> status == HttpStatus.NOT_FOUND, clientResponse -> {
                    log.debug("Card not found: " + maskPAN(pan));
                    return Mono.error(new CardNotFoundException("Card not found: " + maskPAN(pan)));
                })
                .onStatus(status -> status == HttpStatus.SERVICE_UNAVAILABLE, clientResponse -> {
                    log.debug("Card Management service unavailable: {}", clientResponse.statusCode());
                    return Mono
                            .error(new ServiceUnavailableException(
                                    "Card Management service unavailable: " + clientResponse.statusCode()));
                })
                .onStatus(status -> !status.is2xxSuccessful(), clientResponse -> {
                    log.debug("Failed to get card. Status: {}", clientResponse.statusCode());
                    return Mono
                            .error(new CardNotFoundException(
                                    "Failed to get card. Status: " + clientResponse.statusCode()));
                })
                .bodyToMono(CardModel.class)
                .block();
        return response;
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
     * {@link ReserveCardException}.
     * </p>
     *
     * @param amount сумма для резервирования в минимальных единицах валюты
     *               (копейки, центы)
     * @param rrn    уникальный идентификатор транзакции (Retrieval Reference
     *               Number)
     * @param pan    номер карты для резервирования средств
     * @throws Exception если резервирование не удалось.
     *                   Причина ошибки содержится в {@link ReserveCardException}
     *
     * @see ReserveRequest
     * @see ReserveCardException
     */
    public void reserve(long amount, String rrn, String pan) throws Exception {
        ReserveRequest reserveRequest = new ReserveRequest(amount, rrn);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String url = cmsUrl + "/api/cards/" + pan + "/reserve";
        log.debug("Reserving amount {} for card {} with rrn {}", amount, maskPAN(pan), rrn);
        String response = webClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(reserveRequest)
                .retrieve()
                .onStatus(status -> !status.is2xxSuccessful(), clientResponse -> {
                    log.debug("Reserve failed. Status: {}", clientResponse.statusCode());
                    return Mono.error(
                            new ReserveCardException("Failed to reserve. Status: " + clientResponse.statusCode()));
                })
                .bodyToMono(String.class)
                .block();

        log.debug("Reserve successful for card {}", maskPAN(pan));
    }

    private final AtomicReference<String> lastTimestampAndSeq = new AtomicReference<>("");

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
        Calendar calendar = Calendar.getInstance();

        String currentSecond = String.format("%1d%03d%02d%02d%02d",
                calendar.get(Calendar.YEAR) % 10,
                calendar.get(Calendar.DAY_OF_YEAR),
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                calendar.get(Calendar.SECOND));

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
        return new Random().ints(6, 0, 36)
                .mapToObj(i -> Character.toString(i < 10 ? '0' + i : 'A' + i - 10))
                .collect(Collectors.joining());
    }

    /**
     * Маскирует номер банковской карты (PAN) для безопасного вывода в логи.
     *
     * <p>
     * Формат маскирования: первые 4 цифры + 8 звездочек + последние 4 цифры.
     * Пример: "1234******5678"
     * </p>
     *
     * <p>
     * Если переданный PAN некорректен (null или длина не равна 16 символам),
     * метод возвращает строку "INVALID_PAN" и записывает предупреждение в лог.
     * </p>
     *
     * @param pan полный номер карты (16 цифр)
     * @return маскированный номер карты в формате "1234******5678"
     *         или "INVALID_PAN" для некорректного PAN
     */
    public String maskPAN(String pan) {
        if (pan == null || pan.length() != 16) {
            log.warn("Invalid PAN provided for masking");
            return "INVALID_PAN";
        }

        return pan.substring(0, 4) + "*".repeat(8) + pan.substring(12);
    }
}

package com.processing.authorization.client;

import com.processing.authorization.exceptions.*;
import com.processing.common.dto.authorization.RollbackRequest;
import com.processing.common.dto.cardmanagement.CardModel;
import com.processing.common.dto.cardmanagement.ReserveRequest;
import com.processing.common.utils.MaskPan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;

@RequiredArgsConstructor
@Slf4j
@Component
public class CardManagementClientImpl implements CardManagementClient {
    private final RestClient restClient;

    @Value("${card-management.url}")
    private String cmsUrl;

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
    @Override
    public CardModel getCard(String pan) {
        URI uri = UriComponentsBuilder
                .fromUriString(cmsUrl)
                .scheme("http")
                .path("/api/cards/{pan}")
                .buildAndExpand(pan)
                .toUri();
        log.debug("Getting card info for pan {}", MaskPan.maskPan(pan));

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
    @Override
    public void reserve(BigDecimal amount, String rrn, String pan) {
        ReserveRequest reserveRequest = new ReserveRequest(amount, rrn);
        URI uri = UriComponentsBuilder
                .fromUriString(cmsUrl)
                .scheme("http")
                .path("/api/cards/{pan}/reserve")
                .buildAndExpand(pan)
                .toUri();
        log.debug("Reserving amount {} for card {} with rrn {}", amount, MaskPan.maskPan(pan), rrn);
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

        log.debug("Reserve successful for card {}", MaskPan.maskPan(pan));
    }

    @Override
    public void rollback(RollbackRequest request) {
        URI uri = UriComponentsBuilder
                .fromUriString(cmsUrl)
                .scheme("http")
                .path("/api/cards/{pan}/rollback")
                .buildAndExpand(request.pan())
                .toUri();
        log.debug("Rollback amount {} for card {} with rrn {}", request.amount(), MaskPan.maskPan(request.pan()), request.rrn());
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
                    throw new CardNotFoundException("Card not found: " + MaskPan.maskPan(request.pan()));
                })
                .onStatus(status -> status.value() == 409, (req, res) -> {
                    throw new RollbackConflictException("Rollback conflict");
                })
                .onStatus(status -> !status.is2xxSuccessful(), (req, res) -> {
                    throw new RollbackFailureException("Failed to rollback. Status: " + res.getStatusCode());
                })
                .toBodilessEntity();

        log.debug("Rollback successful for card {}", MaskPan.maskPan(request.pan()));
    }
}

package com.processing.authorization.controller;

import com.processing.common.dto.authorization.AuthorizationRequest;
import com.processing.common.dto.authorization.AuthorizationResponse;
import com.processing.authorization.services.AuthService;
import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST-контроллер для обработки запросов на авторизацию банковских карт.
 * <p>
 * Предоставляет эндпоинт для выполнения авторизации транзакций по картам.
 * Контроллер принимает запросы на авторизацию, делегирует бизнес-логику
 * сервису {@link AuthService} и формирует HTTP-ответ с соответствующим
 * статус-кодом в зависимости от результата авторизации.
 * </p>
 * <p>
 * Все эндпоинты доступны по базовому пути {@code /api/internal}.
 * </p>
 *
 * @author core-auth-team
 * @see AuthService
 * @see AuthorizationRequest
 * @see AuthorizationResponse
 */
@Slf4j
@RestController
@RequestMapping("/api/internal")
@RequiredArgsConstructor
@Tag(name = "Authorization", description = "Endpoint for authorizing cards")
public class AuthController {
    private final AuthService authService;

        /**
         * Обрабатывает запрос на авторизацию банковской карты.
         *
         * <p>
         * Метод выполняет следующие действия:
         * </p>
         * <ol>
         * <li>Фиксирует время начала обработки запроса</li>
         * <li>Делегирует авторизацию сервису
         * {@link AuthService#authorize(AuthorizationRequest, LocalDateTime)}</li>
         * <li>Вычисляет время обработки запроса в миллисекундах</li>
         * <li>Устанавливает время обработки в ответе</li>
         * <li>Определяет HTTP-статус на основе результата авторизации</li>
         * <li>Возвращает ResponseEntity с соответствующим статусом и телом ответа</li>
         * </ol>
         *
         * <p>
         * Запрос проходит валидацию согласно аннотации {@link Valid}.
         * В случае нарушения ограничений, заданных в {@link AuthorizationRequest},
         * будет возвращен ответ с ошибкой валидации.
         * </p>
         *
         * @param request запрос на авторизацию, содержащий:
         *                <ul>
         *                <li><b>pan</b> - номер карты (16 цифр)</li>
         *                <li><b>amount</b> - сумма транзакции</li>
         *                <li>другие параметры транзакции</li>
         *                </ul>
         *                Запрос должен проходить валидацию ({@link Valid})
         * @return {@link ResponseEntity} с объектом {@link AuthorizationResponse},
         *         содержащим:
         *         <ul>
         *         <li><b>status</b> - статус авторизации (APPROVED или DECLINED)</li>
         *         <li><b>declineReason</b> - причина отклонения (если статус
         *         DECLINED)</li>
         *         <li><b>responseCode</b> - код ответа (двузначный ISO-код)</li>
         *         <li><b>rrn</b> - Retrieval Reference Number (при успешной
         *         авторизации)</li>
         *         <li><b>authCode</b> - код авторизации (при успешной авторизации)</li>
         *         <li><b>processingTimeMs</b> - время обработки запроса в
         *         12:59
         *         миллисекундах</li>
         *         </ul>
         *         HTTP-статус зависит от результата.
         *
         * @see AuthService#authorize(AuthorizationRequest, LocalDateTime)
         * @see AuthorizationRequest
         * @see AuthorizationResponse
         */
        @PostMapping("/authorize")
        @Operation(summary = "Authorization", description = "Approves or declines card by pan")
        @ApiResponses(value = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Authorization success",
                        content = @Content(schema = @Schema(implementation = AuthorizationResponse.class))),
                @ApiResponse(
                        responseCode = "400",
                        description = "Incorrect request or unknown error",
                        content = @Content(schema = @Schema(implementation = AuthorizationResponse.class))),
                @ApiResponse(
                        responseCode = "403",
                        description = "Card blocked, inactive or expired",
                        content = @Content(schema = @Schema(implementation = AuthorizationResponse.class))),
                @ApiResponse(
                        responseCode = "404",
                        description = "Card not found",
                        content = @Content(schema = @Schema(implementation = AuthorizationResponse.class))),
                @ApiResponse(
                        responseCode = "422",
                        description = "Insufficient funds ",
                        content = @Content(schema = @Schema(implementation = AuthorizationResponse.class))),
                @ApiResponse(
                        responseCode = "503",
                        description = "Card manager unavailable",
                        content = @Content(schema = @Schema(implementation = AuthorizationResponse.class)))
        })
        public ResponseEntity<AuthorizationResponse> authorize(@Valid @RequestBody AuthorizationRequest request) {
                LocalDateTime requestInputTime = LocalDateTime.now();
                AuthorizationResponse response = authService.authorize(request, requestInputTime);

                boolean isApproved = response.status().equals(AuthorizationResponse.STATUS_APPROVED);
                HttpStatus httpStatus;
                if (isApproved) {
                        httpStatus = HttpStatus.OK;
                } else {
                        httpStatus = switch (response.declineReason()) {
                                case "CARD_NOT_FOUND" -> HttpStatus.NOT_FOUND;
                                case "SERVICE_UNAVAILABLE", "RESERVATION_FAILED" -> HttpStatus.SERVICE_UNAVAILABLE;
                                case "INSUFFICIENT_FUNDS" -> HttpStatus.UNPROCESSABLE_ENTITY;
                                case "CARD_EXPIRED", "CARD_BLOCKED", "CARD_INACTIVE" -> HttpStatus.FORBIDDEN;
                                default -> HttpStatus.BAD_REQUEST;
                        };
                }
                return ResponseEntity.status(httpStatus).body(response);
        }
}

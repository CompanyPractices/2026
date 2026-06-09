package com.processing.authorization.controller;

import com.processing.authorization.dto.HealthResponse;
import com.processing.authorization.services.HealthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * REST-контроллер для проверки работоспособности сервиса авторизации и его
 * зависимостей.
 * <p>
 * Предоставляет эндпоинт для мониторинга состояния сервиса и всех внешних
 * систем,
 * от которых он зависит. Используется для автоматического определения
 * доступности
 * сервиса в системах оркестрации и балансировки нагрузки.
 * </p>
 * <p>
 * Возвращает HTTP 200, если все зависимости здоровы, или HTTP 503,
 * если хотя бы одна зависимость недоступна.
 * </p>
 *
 * @author core-auth-team
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "Health Check", description = "Endpoint for checking service health and dependencies")
public class HealthController {
    private final HealthService healthService;

    /**
     * Выполняет проверку работоспособности сервиса авторизации и всех его
     * зависимостей.
     *
     * <p>
     * Метод агрегирует результаты проверки всех внешних сервисов и формирует
     * итоговый статус. Если все сервисы возвращают статус "ok", то общий статус
     * считается "ok" и возвращается HTTP 200. В противном случае статус "degraded"
     * и HTTP 503.
     * </p>
     *
     * @return {@link ResponseEntity} с объектом {@link HealthResponse}, содержащим:
     *         <ul>
     *         <li><b>status</b> - общий статус ("ok" или "degraded")</li>
     *         <li><b>service</b> - имя текущего сервиса ("authorization")</li>
     *         <li><b>dependencies</b> - карта статусов всех зависимых сервисов,
     *         где ключ - URL сервиса, значение - его статус</li>
     *         </ul>
     *
     * @see HealthService#healthCheckAllServices()
     * @see HealthResponse
     */
    @GetMapping("/health")
    @Operation(summary = "Health check endpoint", description = "Returns the health status of the authorization service and all its dependencies")
    @ApiResponses(value = {
        @ApiResponse(
                responseCode = "200",
                description = "All services are healthy",
                content = @Content(schema = @Schema(implementation = HealthResponse.class))),
        @ApiResponse(
                responseCode = "503",
                description = "Service is degraded (one or more dependencies are unhealthy)",
                content = @Content(schema = @Schema(implementation = HealthResponse.class)))
    })
    public ResponseEntity<HealthResponse> health() {
        Map<String, String> checks = healthService.healthCheckAllServices();
        boolean isAllHealthy = checks.values().stream()
                .allMatch(status -> "ok".equalsIgnoreCase(status));
        HttpStatus httpStatus = isAllHealthy ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(httpStatus).body(new HealthResponse(
                isAllHealthy ? "ok" : "degraded",
                "authorization",
                checks));
    }
}

package com.processing.authorization.services;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.client.RestClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Record для представления ответа health-эндпоинта внешнего сервиса.
 *
 * @param service имя сервиса (например, "card-management")
 * @param status  статус сервиса (например, "ok", "down", "degraded")
 */
record Response(String service, String status) {
}

/**
 * Сервис для выполнения проверки работоспособности внешних зависимостей.
 * <p>
 * Выполняет HTTP-запросы к health-эндпоинтам всех сконфигурированных внешних
 * сервисов
 * и собирает их статусы. Использует реактивный WebClient.
 * </p>
 * <p>
 * Список проверяемых сервисов задается через конфигурационный параметр
 * {@code services-to-health-check}.
 * </p>
 *
 * @author core-auth-team
 * @see Response
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HealthService {
    @Value("${services-to-health-check}")
    private List<String> toHealthCheck;
    private final RestClient restClient;

    /**
     * Выполняет проверку работоспособности всех сконфигурированных внешних
     * сервисов.
     *
     * <p>
     * Для каждого URL из списка {@code toHealthCheck} выполняется запрос к
     * эндпоинту
     * {@code /health}. Результаты агрегируются в карту, где ключом является имя
     * сервиса,
     * а значением - его статус.
     * </p>
     *
     * <p>
     * В случае ошибки при проверке конкретного сервиса, ему присваивается статус
     * "down",
     * но проверка остальных сервисов продолжается.
     * </p>
     *
     * @return {@link Map}, где:
     *         <ul>
     *         <li><b>ключ</b> - имя сервиса (из ответа health-эндпоинта или
     *         URL)</li>
     *         <li><b>значение</b> - статус сервиса ("ok", "down", "unknown" и
     *         др.)</li>
     *         </ul>
     *
     * @see #checkHealth(String)
     */
    public Map<String, String> healthCheckAllServices() {
        Map<String, String> result = new HashMap<>();
        log.debug("Starting to health check depended services");
        for (String serviceUrl : toHealthCheck) {
            Response healthCheckResponse = checkHealth(serviceUrl);
            result.put(healthCheckResponse.service(), healthCheckResponse.status());
        }
        return result;
    }

    private Response checkHealth(String serviceUrl) {
        try {
            log.debug("Checking health of {}", serviceUrl);
            URI uri = UriComponentsBuilder
                    .fromUriString(serviceUrl)
                    .scheme("http")
                    .path("/health")
                    .build()
                    .toUri();

            Map<String, Object> body = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .onStatus(status -> !status.is2xxSuccessful(), (req, res) -> {
                        log.debug("Health check failed for {}", uri);
                        throw new RuntimeException("Health check failed for " + uri);
                    })
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {
                    });

            if (body == null) {
                return new Response(serviceUrl, "unknown");
            }

            Object statusObj = body.get("status");
            Object serviceObj = body.get("service");
            String status = statusObj instanceof String ? (String) statusObj : "unknown";
            String service = serviceObj instanceof String ? (String) serviceObj : serviceUrl;
            return new Response(service, status);
        } catch (Exception e) {
            log.error("Health check failed for {}", serviceUrl, e);
            return new Response(serviceUrl, "down");
        }
    }
}

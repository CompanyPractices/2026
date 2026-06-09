package com.processing.gateway.service;

import com.processing.gateway.properties.HealthProperties;
import com.processing.gateway.properties.ServiceProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class HealthService {
    private static final String HEALTH_ROUTE = "/health";
    private static final String HEALTH_OK = "ok";
    private static final String HEALTH_DOWN = "down";

    private static final String SWITCH_SERVICE_NAME = "switch";
    private static final String AUTH_SERVICE_NAME = "authorization";
    private static final String CARD_MGMT_SERVICE_NAME = "cardManagement";
    private static final String LOGGER_SERVICE_NAME = "logger";

    private final HttpClient httpClient;
    private final ServiceProperties serviceProperties;
    private final HealthProperties healthProperties;

    public Map<String, String> getDownstreamServicesHealth() {
        String switchUrl = serviceProperties.getSwitchUrl() + HEALTH_ROUTE;
        String loggerUrl = serviceProperties.getLoggerUrl() + HEALTH_ROUTE;
        String authUrl = serviceProperties.getAuthUrl() + HEALTH_ROUTE;
        String cardsUrl = serviceProperties.getCardsUrl() + HEALTH_ROUTE;

        return Map.of(
                SWITCH_SERVICE_NAME, checkService(switchUrl),
                AUTH_SERVICE_NAME, checkService(authUrl),
                CARD_MGMT_SERVICE_NAME, checkService(cardsUrl),
                LOGGER_SERVICE_NAME, checkService(loggerUrl));
    }

    private String checkService(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(healthProperties.getRequestTimeout()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return HEALTH_OK;
            }

            return HEALTH_DOWN;
        } catch (Exception e) {
            return HEALTH_DOWN;
        }
    }
}

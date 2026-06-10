package com.processing.gateway.service;

import com.processing.gateway.dto.HealthResponse;
import com.processing.gateway.enums.HealthStatus;
import com.processing.gateway.properties.GatewayProperties;
import com.processing.gateway.properties.HealthProperties;
import com.processing.gateway.properties.ServiceProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
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
    private static final String GATEWAY_SERVICE_NAME = "gateway";
    private static final String SWITCH_SERVICE_NAME = "switch";
    private static final String AUTH_SERVICE_NAME = "authorization";
    private static final String CARD_MGMT_SERVICE_NAME = "cardManagement";
    private static final String LOGGER_SERVICE_NAME = "logger";

    private final HttpClient httpClient;
    private final ServiceProperties serviceProperties;
    private final HealthProperties healthProperties;
    private final GatewayProperties gatewayProperties;

    public HealthResponse getDownstreamServicesHealth() {
        String switchUrl = serviceProperties.getSwitchUrl() + healthProperties.getUrl();
        String loggerUrl = serviceProperties.getLoggerUrl() + healthProperties.getUrl();
        String authUrl = serviceProperties.getAuthUrl() + healthProperties.getUrl();
        String cardsUrl = serviceProperties.getCardsUrl() + healthProperties.getUrl();

        return new HealthResponse(
                HealthStatus.OK.name().toLowerCase(),
                GATEWAY_SERVICE_NAME,
                gatewayProperties.getVersion(),
                Map.of(
                    SWITCH_SERVICE_NAME, checkService(switchUrl),
                    AUTH_SERVICE_NAME, checkService(authUrl),
                    CARD_MGMT_SERVICE_NAME, checkService(cardsUrl),
                    LOGGER_SERVICE_NAME, checkService(loggerUrl)));
    }

    private String checkService(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(healthProperties.getRequestTimeout()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == HttpStatus.OK.value()) {
                return HealthStatus.OK.name().toLowerCase();
            }

            return HealthStatus.UNAVAILABLE.name().toLowerCase();
        } catch (Exception e) {
            return HealthStatus.UNAVAILABLE.name().toLowerCase();
        }
    }
}

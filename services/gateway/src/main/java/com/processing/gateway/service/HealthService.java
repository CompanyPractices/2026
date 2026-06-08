package com.processing.gateway.service;

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
    private static final Duration HEALTH_REQUEST_TIMEOUT = Duration.ofSeconds(2);
    private final HttpClient httpClient;
    private final ServiceProperties serviceProperties;

    public Map<String, String> getDownstreamServicesHealth() {
        String switchUrl = serviceProperties.getSwitchUrl() + "/health";
        String loggerUrl = serviceProperties.getLoggerUrl() + "/health";
        String authUrl = serviceProperties.getAuthUrl() + "/health";
        String cardsUrl = serviceProperties.getCardsUrl() + "/health";

        return Map.of("switch", checkService(switchUrl),
                "authorization", checkService(authUrl),
                "cardManagement", checkService(cardsUrl),
                "logger", checkService(loggerUrl));
    }

    private String checkService(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create(url))
                    .timeout(HEALTH_REQUEST_TIMEOUT)
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return "ok";
            }

            return "down";
        } catch (Exception e) {
            return "down";
        }
    }
}

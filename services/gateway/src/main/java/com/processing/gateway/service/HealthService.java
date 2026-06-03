package com.processing.gateway.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.processing.gateway.properties.ServiceProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class HealthService {
    private final HttpClient httpClient;
    private final ServiceProperties serviceProperties;
    private final ObjectMapper mapper;

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
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode root = mapper.readTree(response.body());
                String status = root.path("status").asText();

                return status.equals("ok") ? "ok" : "down";
            }

            return "down";
        } catch (Exception e) {
            return "down";
        }
    }
}

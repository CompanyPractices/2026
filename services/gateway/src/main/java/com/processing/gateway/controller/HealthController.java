package com.processing.gateway.controller;

import com.processing.gateway.dto.HealthResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class HealthController {
    private final HttpClient httpClient;
    // private final WebClient webClient;

    @GetMapping("/health")
    public ResponseEntity<HealthResponse> health() {
        String switchUrl = System.getenv("SWITCH");
        String loggerUrl = System.getenv("LOGGER");
        String authUrl = System.getenv("AUTH");
        String cardsUrl = System.getenv("CARDS");

        return ResponseEntity.ok(new HealthResponse(
                "ok",
                "gateway",
                "1.0.0",
                Map.of("switch", checkService(switchUrl),
                        "authorization", checkService(authUrl),
                        "cardManagement", checkService(cardsUrl),
                        "logger", checkService(loggerUrl))
        ));
    }

    private String checkService(String url) {
        try {
            var response = httpClient.send(HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create(url))
                    .build(), HttpResponse.BodyHandlers.ofString());

            if (response.body().contains("\"status\": \"ok\""))
                return "ok";

            return "down";
        } catch (Exception e) {
            return "down";
        }
    }

//    private Mono<String> checkService(String url) {
//        return webClient.get()
//                .uri(url)
//                .retrieve()
//                .toBodilessEntity()
//                .map(x -> "ok")
//                .onErrorReturn("down");
//    }
}

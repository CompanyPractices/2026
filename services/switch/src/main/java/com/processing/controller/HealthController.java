package com.processing.controller;

import com.processing.config.SwitchProperties;
import com.processing.dto.HealthResponse;
import com.processing.service.AuthorizationClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {

    private final SwitchProperties switchProperties;
    private final AuthorizationClient authorizationClient;

    public HealthController(SwitchProperties switchProperties, AuthorizationClient authorizationClient) {
        this.switchProperties = switchProperties;
        this.authorizationClient = authorizationClient;
    }

    @GetMapping("/health")
    public ResponseEntity<HealthResponse> health() {
        return ResponseEntity.ok(new HealthResponse(
                "ok",
                "switch",
                switchProperties.version(),
                Map.of("authorization", authorizationClient.checkHealth())
        ));
    }
}

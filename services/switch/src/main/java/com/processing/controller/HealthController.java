package com.processing.controller;

import com.processing.config.SwitchProperties;
import com.processing.dto.HealthResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {

    private final SwitchProperties switchProperties;

    public HealthController(SwitchProperties switchProperties) {
        this.switchProperties = switchProperties;
    }

    @GetMapping("/health")
    public ResponseEntity<HealthResponse> health() {
        return ResponseEntity.ok(new HealthResponse(
                "ok",
                "switch",
                switchProperties.version(),
                Map.of("authorization", "ok")
        ));
    }
}

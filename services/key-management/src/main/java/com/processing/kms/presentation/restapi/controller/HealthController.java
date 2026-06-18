package com.processing.kms.presentation.restapi.controller;

import com.processing.kms.presentation.restapi.dto.HealthResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<HealthResponse> health() {
        return ResponseEntity.ok(new HealthResponse(
                "ok",
                "apiKey",
                Map.of()
        ));
    }
}

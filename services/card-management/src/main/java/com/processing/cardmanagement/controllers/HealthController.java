package com.processing.cardmanagement.controllers;

import com.processing.cardmanagement.models.HealthResponse;
import com.processing.cardmanagement.services.CardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class HealthController {

    private final CardService cardService;

    @GetMapping("/health")
    public ResponseEntity<HealthResponse> health() {
        return ResponseEntity.ok(new HealthResponse(
            "ok",
            "card-management",
            cardService.countCards()
        ));
    }
}

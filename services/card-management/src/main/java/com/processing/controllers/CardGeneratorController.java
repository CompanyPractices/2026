package com.processing.controllers;

import com.processing.models.CardDto;
import com.processing.models.GenerateCardResponse;
import com.processing.models.GenerateCardsRequest;
import com.processing.services.CardGeneratorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
public class CardGeneratorController {
    private final CardGeneratorService generatorService;

    @PostMapping("/generate")
    public ResponseEntity<GenerateCardResponse> generate(@Valid @RequestBody GenerateCardsRequest request) {
        List<CardDto> result = generatorService.generate(request.count(), request.bins());

        return ResponseEntity.status(201).body(new GenerateCardResponse(result.size(), result));
    }
}

package com.processing.cardmanagement.controllers;

import com.processing.cardmanagement.annotations.Pan;
import com.processing.cardmanagement.models.*;
import com.processing.cardmanagement.services.CardService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cards")
@Validated
public class CardController {

    private final CardService cardService;

    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    @PostMapping("/")
    public ResponseEntity<CardDto> createCard(@Valid @RequestBody CreateCardRequest data) {
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(cardService.createCard(data));
    }

    @GetMapping("/{pan}")
    public ResponseEntity<CardDto> getCard(@PathVariable @Pan String pan) {
        return ResponseEntity.ok(cardService.getCard(pan));
    }

    @GetMapping("/")
    public ResponseEntity<List<CardDto>> getCards(@Valid @RequestBody GetCardsRequest data) {
        return ResponseEntity.ok(cardService.getCards(data));
    }

    @PatchMapping("/{pan}")
    public ResponseEntity<Void> patchCard(
        @PathVariable @Pan String pan,
        @Valid @RequestBody PatchCardRequest data
    ) {
        cardService.patchCard(pan, data);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{pan}")
    public ResponseEntity<Void> deleteCard(@PathVariable @Pan String pan) {
        cardService.deleteCard(pan);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{pan}/reserve")
    public ResponseEntity<Void> reserve(
        @PathVariable @Pan String pan,
        @Valid @RequestBody ReserveRequest data
    ) {
        cardService.reserve(pan, data);
        return ResponseEntity.ok().build();
    }
}

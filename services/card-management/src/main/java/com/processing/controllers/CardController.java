package com.processing.controllers;

import com.processing.annotations.Pan;
import com.processing.models.*;
import com.processing.services.CardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cards")
@Validated
@Tag(name = "Cards", description = "Card management endpoints")
public class CardController {

    private final CardService cardService;

    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    @Operation(summary = "Create a new card")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Card create successfully"),
            @ApiResponse(responseCode = "400", description = "invalid request data")
    })
    @PostMapping("/")
    public ResponseEntity<CardDto> createCard(@Valid @RequestBody CreateCardRequest data) {
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(cardService.createCard(data));
    }

    @Operation(summary = "Get card by PAN")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Card found"),
            @ApiResponse(responseCode = "400", description = "Invalid PAN format"),
            @ApiResponse(responseCode = "404", description = "Card not found")
    })
    @GetMapping("/{pan}")
    public ResponseEntity<CardDto> getCard(@PathVariable @Pan String pan) {
        return ResponseEntity.ok(cardService.getCard(pan));
    }

    @Operation(summary = "Get list of cards with pagination and filters")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cards retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
    })
    @GetMapping("/")
    public ResponseEntity<List<CardDto>> getCards(@Valid @RequestBody GetCardsRequest data) {
        return ResponseEntity.ok(cardService.getCards(data));
    }

    @Operation(summary = "Partially update a card")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Card update successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "404", description = "Card not found")
    })
    @PatchMapping("/{pan}")
    public ResponseEntity<Void> patchCard(
        @PathVariable @Pan String pan,
        @Valid @RequestBody PatchCardRequest data
    ) {
        cardService.patchCard(pan, data);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Delete a card (sets status to DELETED)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Card deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid PAN format"),
            @ApiResponse(responseCode = "404", description = "Card not found")
    })
    @DeleteMapping("/{pan}")
    public ResponseEntity<Void> deleteCard(@PathVariable @Pan String pan) {
        cardService.deleteCard(pan);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Reserve funds on a card")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Funds reserve successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "402", description = "Insufficient funds"),
            @ApiResponse(responseCode = "404", description = "Card not found")
    })
    @PostMapping("/{pan}/reserve")
    public ResponseEntity<Void> reserve(
        @PathVariable @Pan String pan,
        @Valid @RequestBody ReserveRequest data
    ) {
        cardService.reserve(pan, data);
        return ResponseEntity.ok().build();
    }
}

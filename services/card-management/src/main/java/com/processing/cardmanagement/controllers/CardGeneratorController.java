package com.processing.cardmanagement.controllers;

import com.processing.cardmanagement.models.CardDto;
import com.processing.cardmanagement.models.ErrorResponse;
import com.processing.cardmanagement.models.GenerateCardResponse;
import com.processing.cardmanagement.models.GenerateCardsRequest;
import com.processing.cardmanagement.services.CardGeneratorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Cards", description = "Card management endpoints")
public class CardGeneratorController {

    private final CardGeneratorService generatorService;

    @Operation(summary = "Generate test cards")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Cards generated successfully",
                    content = @Content(schema = @Schema(implementation = GenerateCardResponse.class))),
            @ApiResponse(responseCode = "400", description = "invalid request data",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/generate")
    public ResponseEntity<GenerateCardResponse> generate(@Valid @RequestBody GenerateCardsRequest request) {
        List<CardDto> result = generatorService.generate(request.count(), request.bins());

        return ResponseEntity.status(201).body(new GenerateCardResponse(result.size(), result));
    }
}

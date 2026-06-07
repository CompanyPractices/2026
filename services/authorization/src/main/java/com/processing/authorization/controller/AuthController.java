package com.processing.authorization.controller;

import com.processing.authorization.dto.AuthorizationRequest;
import com.processing.authorization.dto.AuthorizationResponse;
import com.processing.authorization.enums.AuthorizationRequestStatus;
import com.processing.authorization.services.AuthService;
import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/internal")
@RequiredArgsConstructor
@Tag(name = "Authorization", description = "Endpoint for authorizing cards")
public class AuthController {
    private final AuthService authService;

    @PostMapping("/authorize")
    @Operation(summary = "Authorization", description = "Approves or declines card by pan")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Authorization success",
                    content = @Content(schema = @Schema(implementation = AuthorizationResponse.class))),
            @ApiResponse(responseCode = "400", description = "Incorrect request or unknown error",
                    content = @Content(schema = @Schema(implementation = AuthorizationResponse.class))),
            @ApiResponse(responseCode = "403", description = "Card blocked, inactive or expired",
                    content = @Content(schema = @Schema(implementation = AuthorizationResponse.class))),
            @ApiResponse(responseCode = "404", description = "Card not found",
                    content = @Content(schema = @Schema(implementation = AuthorizationResponse.class))),
            @ApiResponse(responseCode = "422", description = "Insufficient funds ",
                    content = @Content(schema = @Schema(implementation = AuthorizationResponse.class))),
            @ApiResponse(responseCode = "503", description = "Card manager unavailable",
                    content = @Content(schema = @Schema(implementation = AuthorizationResponse.class)))
    })
    public ResponseEntity<AuthorizationResponse> authorize(@Valid @RequestBody AuthorizationRequest request) {
        AuthorizationResponse response = authService.authorize(request);
        boolean isApproved = response.getStatus().equals(AuthorizationRequestStatus.APPROVED);

        HttpStatus httpStatus;
        if (isApproved) {
            httpStatus = HttpStatus.OK;
        } else {
            httpStatus = switch (response.getDeclineReason()) {
                case "CARD_NOT_FOUND" -> HttpStatus.NOT_FOUND;
                case "SERVICE_UNAVAILABLE", "RESERVATION_FAILED" -> HttpStatus.SERVICE_UNAVAILABLE;
                case "INSUFFICIENT_FUNDS" -> HttpStatus.UNPROCESSABLE_ENTITY;
                case "CARD_EXPIRED", "CARD_BLOCKED", "CARD_INACTIVE" -> HttpStatus.FORBIDDEN;
                default -> HttpStatus.BAD_REQUEST;
            };
        }
        return ResponseEntity.status(httpStatus).body(response);
    }
}

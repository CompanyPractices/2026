package com.processing.controller;

import java.time.LocalDate;
import com.processing.enums.CardStatus;
import com.processing.dto.*;
import com.processing.services.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import java.lang.Exception;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/api/internal/authorize")
    public AuthorizationResponse authorize(@RequestBody AuthorizationRequest request) throws Exception {
        CardResponse cardResponse = authService.getCard(request.getPan());

        CardStatus currCardStatus = cardResponse.getStatus();
        if (!currCardStatus.equals(CardStatus.ACTIVE)) {
            return switch (currCardStatus) {
                case EXPIRED -> AuthorizationResponse.declined(request, "CARD_EXPIRED", "54");
                case BLOCKED -> AuthorizationResponse.declined(request, "CARD_BLOCKED", "05");
                case INACTIVE -> AuthorizationResponse.declined(request, "CARD_INACTIVE", "05");
                default -> AuthorizationResponse.declined(request, "UNKNOWN_REASON", "05");
            };
        }

        if (LocalDate.parse(cardResponse.getExpiryDate()).isBefore(LocalDate.now())) {
            return AuthorizationResponse.declined(request, "CARD_EXPIRED", "54");
        }

        // TODO check daily limit when add table limit_usage

        // TODO check month limit when add table limit_usage

        if (request.getAmount() > cardResponse.getAvailableBalance())
            return AuthorizationResponse.declined(request, "INSUFFICIENT_FUNDS", "51");

        String rrn = authService.generateRRN();
        authService.reserve(request.getAmount(), rrn, request.getPan());
        String authCode = authService.generateAuthCode();
        return AuthorizationResponse.approved(request, rrn, authCode);
    }
}

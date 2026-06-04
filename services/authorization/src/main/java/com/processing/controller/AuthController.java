package com.processing.controller;

import java.time.LocalDate;
import java.util.concurrent.TimeoutException;

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
        return authService.authorize(request);
    }
}

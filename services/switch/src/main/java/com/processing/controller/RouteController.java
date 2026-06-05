package com.processing.controller;

import com.processing.model.AuthorizationRequest;
import com.processing.model.AuthorizationResponse;
import com.processing.service.RouteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal")
public class RouteController {

    private static final Logger log = LoggerFactory.getLogger(RouteController.class);

    private final RouteService routeService;

    public RouteController(RouteService routeService) {
        this.routeService = routeService;
    }

    @PostMapping("/route")
    public ResponseEntity<AuthorizationResponse> routeTransaction(
            @RequestBody AuthorizationRequest request) {
        log.info("Received: STAN={}, PAN={}", request.stan(), maskPan(request.pan()));
        AuthorizationResponse response = routeService.route(request);
        return ResponseEntity.ok(response);
    }

    private String maskPan(String pan) {
        if (pan == null || pan.length() < 8) {
            return "****";
        }
        return pan.substring(0, 4) + "****" + pan.substring(pan.length() - 4);
    }
}

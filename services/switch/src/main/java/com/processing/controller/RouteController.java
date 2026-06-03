package com.processing.controller;


import com.processing.model.AuthorizationRequest;
import com.processing.model.AuthorizationResponse;
import com.processing.service.RoutingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@Slf4j
@RestController
@RequestMapping("/api/internal")
@RequiredArgsConstructor
public class RouteController {


    private final RoutingService routingService;


    @PostMapping("/route")
    public ResponseEntity<AuthorizationResponse> routeTransaction(
            @RequestBody AuthorizationRequest request) {


        log.info("Received: STAN={}, PAN={}", request.getStan(), maskPan(request.getPan()));


        String issuerId = routingService.getIssuerIdByPan(request.getPan());


        if (issuerId == null) {
            log.warn("Unknown BIN: {}", maskPan(request.getPan()));


            AuthorizationResponse response = AuthorizationResponse.builder()
                    .mti("0110")
                    .stan(request.getStan())
                    .responseCode("14")
                    .status("DECLINED")
                    .declineReason("Invalid card number (unknown BIN)")
                    .processingTimeMs(0)
                    .build();


            return ResponseEntity.ok(response);
        }


        request.setIssuerId(issuerId);


        // 4. TODO: Отправить в Authorization Service (пока заглушка)
        //    Временно возвращаем APPROVED для тестирования


        AuthorizationResponse response = AuthorizationResponse.builder()
                .mti("0110")
                .stan(request.getStan())
                .rrn("012345678901")
                .authCode("TEST01")
                .responseCode("00")
                .status("APPROVED")
                .processingTimeMs(42)
                .build();


        log.info("Response: {} -> {}", request.getStan(), response.getStatus());


        return ResponseEntity.ok(response);
    }


    private String maskPan(String pan) {
        if (pan == null || pan.length() < 8) return "****";
        return pan.substring(0, 4) + "****" + pan.substring(pan.length() - 4);
    }
}

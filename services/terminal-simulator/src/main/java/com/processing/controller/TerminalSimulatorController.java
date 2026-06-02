package com.processing.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@RestController
@RequestMapping("/api/simulator/terminal")
public class TerminalSimulatorController {

    @PostMapping("/run")
    public ResponseEntity<Map<String, Object>> run(@RequestBody Map<String, Object> request) {
        int count = ((Number) request.getOrDefault("count", 2)).intValue();
        String scenario = (String) request.getOrDefault("scenario", "normal");

        long start = System.currentTimeMillis();
        List<Map<String, Object>> transactions = new ArrayList<>();
        int approved = 0, declined = 0;

        for (int i = 0; i < count; i++) {
            Map<String, Object> tx = generateTransaction(scenario);
            transactions.add(tx);

            Map<String, Object> authResp = sendToGatewayMock(tx);
            if ("APPROVED".equals(authResp.get("status"))) approved++;
            else declined++;
        }

        long elapsed = System.currentTimeMillis() - start;
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("totalSubmitted", count);
        response.put("approved", approved);
        response.put("declined", declined);
        response.put("elapsedMs", elapsed);
        response.put("transactions", transactions);
        return ResponseEntity.ok(response);
    }

    private Map<String, Object> generateTransaction(String scenario) {
        Map<String, Object> tx = new LinkedHashMap<>();
        tx.put("mti", "0100");
        tx.put("stan", "000001");
        tx.put("pan", "4000001234560001");
        tx.put("processingCode", "000000");
        tx.put("amount", 125000);
        tx.put("currencyCode", "643");
        tx.put("transmissionDateTime", "2026-06-01T14:30:00Z");
        tx.put("terminalId", "TERM001");
        tx.put("terminalType", "POS");
        tx.put("merchantId", "MERCH12345678901");
        tx.put("mcc", "5411");
        tx.put("acquirerId", "ACQ001");
        return tx;
    }

    private Map<String, Object> sendToGatewayMock(Map<String, Object> tx) {
        RestTemplate rest = new RestTemplate();
        String gatewayUrl = "http://gateway:8080/api/transactions";
        try {
            ResponseEntity<Map> response = rest.postForEntity(gatewayUrl, tx, Map.class);
            return response.getBody();
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "DECLINED");
            errorResponse.put("responseCode", "96");
            errorResponse.put("errorMessage", "Gateway unavailable " + e.getMessage());
            return errorResponse;
        }
    }

}

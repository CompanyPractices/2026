package com.processing.service;

import com.processing.dto.RunResponse;
import com.processing.model.Transaction;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class TerminalSimulatorService {
    public RunResponse run(int count, String scenario) {
        long start = System.currentTimeMillis();
        List<Transaction> transactions = new ArrayList<>();
        int approved = 0, declined = 0;

        for (int i = 0; i < count; i++) {
            Transaction tx = generateTransaction(scenario);
            transactions.add(tx);

            Map<String, Object> authResp = sendToGateway(tx);
            if ("APPROVED".equals(authResp.get("status"))) approved++;
            else declined++;
        }

        long elapsed = System.currentTimeMillis() - start;
        return new RunResponse(count, approved, declined, elapsed, transactions);
    }

    private Transaction generateTransaction(String scenario) {
        return new Transaction("0100", "000001", "4000001234560001",
                "000000", 125000, "643", "2026-06-01T14:30:00Z",
                "TERM001", "POS", "MERCH12345678901", "5411", "ACQ001");
    }

    private Map<String, Object> sendToGateway(Transaction tx) {
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

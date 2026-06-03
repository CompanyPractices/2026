package com.processing.service;

import com.processing.dto.AuthorizationResponse;
import com.processing.dto.RunResponse;
import com.processing.dto.AuthorizationRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class TerminalSimulatorService {
    public RunResponse run(int count, String scenario) {
        long start = System.currentTimeMillis();
        List<AuthorizationResponse> authResps = new ArrayList<>();
        int approved = 0, declined = 0;

        for (int i = 0; i < count; i++) {
            AuthorizationRequest tx = generateTransaction(scenario);
            AuthorizationResponse authResp = sendToGateway(tx);

            authResps.add(authResp);
            if ("APPROVED".equals(authResp.getStatus())) approved++;
            else declined++;
        }

        long elapsed = System.currentTimeMillis() - start;
        return new RunResponse(count, approved, declined, elapsed, authResps);
    }

    private AuthorizationRequest generateTransaction(String scenario) {
        return new AuthorizationRequest("0100", "000001", "4000001234560001",
                "000000", 125000, "643", "2026-06-01T14:30:00Z",
                "TERM001", "POS", "MERCH12345678901", "5411", "ACQ001");
    }

    private AuthorizationResponse sendToGateway(AuthorizationRequest tx) {
        RestTemplate rest = new RestTemplate();
        String gatewayUrl = "http://gateway:8080/api/transactions";
        try {
            ResponseEntity<AuthorizationResponse> response = rest.postForEntity(gatewayUrl, tx, AuthorizationResponse.class);
            return response.getBody();
        } catch (Exception e) {
            AuthorizationResponse errorResponse = new AuthorizationResponse();
            errorResponse.setStatus("DECLINED");
            errorResponse.setResponseCode("505");
            errorResponse.setDeclineReason(e.getMessage());
            return errorResponse;
        }
    }
}

package com.processing.merchantacquirer.service;

import com.processing.merchantacquirer.dto.AuthorizationResponse;
import com.processing.merchantacquirer.dto.SimulatorRequest;
import com.processing.merchantacquirer.dto.SimulatorResponse;
import com.processing.merchantacquirer.dto.TransactionResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.http.HttpRequest;
import java.util.List;

@Service
public class SimulationService {
    public SimulatorResponse run(SimulatorRequest request){
        AuthorizationResponse transaction = new AuthorizationResponse("0100", "000001", "4000001234560001", "000000", 125000, "643", "2026-06-01T14:30:00Z", "TERM001", "POS", "MERCH12345678901", "5411", "ACQ001");

        List<AuthorizationResponse> transactions = List.of(transaction);

        RestTemplate restTemplate = new RestTemplate();

        SimulatorResponse simulatorResponse = null;
        try {
            ResponseEntity<TransactionResponse> response = restTemplate.postForEntity("http://gateway:8080/api/transactions", transaction, TransactionResponse.class);
            if (response.getBody().status().equals("APPROVED")) {
                simulatorResponse = new SimulatorResponse(1, 1, 0, 228, transactions);
            }
        } catch (Exception e) {
            simulatorResponse = new SimulatorResponse(1, 0, 1, 999, transactions);
        }
        return simulatorResponse;
    }
}

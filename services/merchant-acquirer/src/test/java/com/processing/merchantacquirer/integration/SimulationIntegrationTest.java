package com.processing.merchantacquirer.integration;

import com.processing.merchantacquirer.controller.dto.AcquirerFeeResponse;
import com.processing.merchantacquirer.controller.dto.HealthResponse;
import com.processing.merchantacquirer.controller.dto.SimulatorResponse;
import com.processing.merchantacquirer.domain.entity.AcquirerFee;
import com.processing.merchantacquirer.domain.entity.Merchant;
import com.processing.merchantacquirer.domain.repository.AcquirerFeeRepositoryPort;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Tag("integration")
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class SimulationIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
    static HttpServer gatewayStub;

    private static final String CARDS_JSON = """
      {"total":2,"cards":[
        {"id":"c1","pan":"4000000000000001","bin":"400000","cardholderName":"IVAN","expiryDate":"1230",
         "status":"ACTIVE","currencyCode":"643","dailyLimit":"0","monthlyLimit":"0",
         "availableBalance":"0","issuerId":"ISS001","createdAt":"2026-01-01T00:00:00"},
        {"id":"c2","pan":"4000000000000002","bin":"400000","cardholderName":"PETR","expiryDate":"1230",
         "status":"ACTIVE","currencyCode":"643","dailyLimit":"0","monthlyLimit":"0",
         "availableBalance":"0","issuerId":"ISS001","createdAt":"2026-01-01T00:00:00"}]}""";
    private static final String APPROVED_JSON = """
      {"mti":"0110","stan":"000001","rrn":"012345678901","authCode":"AUTH01",
       "responseCode":"00","status":"APPROVED","declineReason":null,"processingTimeMs":5}""";

    @BeforeAll
    static void startGatewayStub() throws IOException {
        gatewayStub = HttpServer.create(new InetSocketAddress(0), 0);
        gatewayStub.createContext("/api/cards", ex -> respond(ex, CARDS_JSON));
        gatewayStub.createContext("/api/transactions", ex -> respond(ex, APPROVED_JSON));
        gatewayStub.setExecutor(Executors.newFixedThreadPool(8));
        gatewayStub.start();
    }

    @AfterAll
    static void stopGatewayStub() {
        gatewayStub.stop(0);
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("gateway.url", () -> "http://localhost:" + gatewayStub.getAddress().getPort());
        registry.add("simulation.sender.concurrency", () -> "10");
        registry.add("simulation.sender.tps", () -> "1000");
    }

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private AcquirerFeeRepositoryPort feeRepository;

    @Test
    void healthReport() {
        ResponseEntity<HealthResponse> response = rest.getForEntity("/health", HealthResponse.class);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("ok", response.getBody().status());
        assertEquals(28L, response.getBody().merchantsLoaded());
    }

    @Test
    void getMerchants() {
        ResponseEntity<Merchant[]> response =
                rest.getForEntity("/api/simulator/merchants", Merchant[].class);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(28, response.getBody().length);
    }

    @Test
    void getAcquirerFee() {
        long before = feeRepository.count();
        ResponseEntity<SimulatorResponse> run = rest.postForEntity(
                "/api/simulator/merchant/run",
                Map.of("count", 5, "scenario", "grocery"),
                SimulatorResponse.class);
        assertEquals(200, run.getStatusCode().value());
        assertNotNull(run.getBody());
        assertEquals(5, run.getBody().totalSubmitted());
        assertEquals(5, run.getBody().approved());
        assertEquals(0, run.getBody().declined());
        assertEquals(before + 5, feeRepository.count());
        AcquirerFee stored = feeRepository.findAll().getFirst();
        ResponseEntity<AcquirerFeeResponse> fee = rest.postForEntity(
                "/api/simulator/merchant/fee",
                Map.of(
                        "transmissionDateTime", stored.getTransmissionDateTime(),
                        "pan", stored.getPan(),
                        "stan", stored.getStan(),
                        "amount", stored.getAmount(),
                        "terminalId", stored.getTerminalId()),
                AcquirerFeeResponse.class);
        assertEquals(200, fee.getStatusCode().value());
        assertNotNull(fee.getBody());
        assertEquals(0, stored.getAcquirerFee().compareTo(fee.getBody().acquirerFee()));
    }

    @Test
    void handleUnknowsRequestForGetAcquirerFee() {
        ResponseEntity<String> response = rest.postForEntity(
                "/api/simulator/merchant/fee",
                Map.of(
                        "transmissionDateTime", "1999-01-01T00:00:00Z",
                        "pan", "4000000000000009",
                        "stan", "999999",
                        "amount", 12345,
                        "terminalId", "TERM9999"),
                String.class);
        assertEquals(404, response.getStatusCode().value());
    }

    private static void respond(HttpExchange exchange, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}

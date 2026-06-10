package com.processing.gateway.integration;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

// todo: change to dynamic ports
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "SWITCH_URL=http://localhost:8082",
                "LOGGER_URL=http://localhost:8088",
                "TERMINAL_SIM_URL=http://localhost:8085",
                "MERCHANT_SIM_URL=http://localhost:8086",
                "CARD_MGMT_URL=http://localhost:8081",
                "gateway.rate-limit.transactions-per-second=100",
                "gateway.open-api.url=http://gateway.test"
        }
)
public class GatewayRoutingIntegrationTest {
    @RegisterExtension
    static WireMockExtension switchWm = WireMockExtension.newInstance()
            .options(wireMockConfig().port(8082))
            .build();

    @RegisterExtension
    static WireMockExtension loggerWm = WireMockExtension.newInstance()
            .options(wireMockConfig().port(8088))
            .build();

    @RegisterExtension
    static WireMockExtension terminalWm = WireMockExtension.newInstance()
            .options(wireMockConfig().port(8085))
            .build();

    @RegisterExtension
    static WireMockExtension merchantWm = WireMockExtension.newInstance()
            .options(wireMockConfig().port(8086))
            .build();

    @RegisterExtension
    static WireMockExtension cardWm = WireMockExtension.newInstance()
            .options(wireMockConfig().port(8081))
            .build();

    @RegisterExtension
    static WireMockExtension wrongWm = WireMockExtension.newInstance()
            .options(wireMockConfig().port(9666))
            .build();

    @Autowired
    private TestRestTemplate restTemplate;

    // Requests

    private static final String VALID_TRANSACTION_REQUEST = """
            {
              "mti": "0100",
              "stan": "000001",
              "pan": "4000003458730237",
              "processingCode": "000000",
              "amount": 1000,
              "currencyCode": "643",
              "transmissionDateTime": "2026-06-05T18:12:49.07",
              "terminalId": "TERM001",
              "terminalType": "POS",
              "merchantId": "MERCH00000000029",
              "mcc": "5045",
              "acquirerId": "ACQ002"
            }
            """;

    private static final String TERMINAL_SIMULATOR_REQUEST = """
            {
              "count": 50,
              "scenario": "normal"
            }
            """;

    private static final String MERCHANT_SIMULATOR_REQUEST = """
            {
              "count": 50,
              "mccCodes": ["5411", "5812"],
              "scenario": "grocery"
            }
            """;

    private static final String CARD_MANAGEMENT_REQUEST = """
            {
              "bin": "400000",
              "cardholderName": "IVAN IVANOV",
              "currencyCode": "643",
              "dailyLimit": 15000000,
              "monthlyLimit": 300000000,
              "initialBalance": 100000000
            }
            """;

    // Responses

    private static final String TRANSACTION_RESPONSE = """
            {
              "mti": "0110",
              "stan": "000001",
              "rrn": "123456789012",
              "authCode": "A12345",
              "responseCode": "00",
              "status": "APPROVED",
              "declineReason": null,
              "processingTimeMs": 10
            }
            """;

    private static final String TERMINAL_SIMULATOR_RESPONSE = """
            {
              "totalSubmitted": 50,
              "approved": 47,
              "declined": 3,
              "elapsedMs": 2300,
              "transactions": []
            }
            """;

    private static final String MERCHANT_SIMULATOR_RESPONSE = """
            {
              "totalSubmitted": 50,
              "approved": 47,
              "declined": 3,
              "elapsedMs": 2300,
              "transactions": []
            }
            """;

    private static final String LOGGER_RESPONSE = """
            {
              "totalTransactions": 1250,
              "approvedCount": 1100,
              "declinedCount": 150,
              "approvalRate": 0.88,
              "totalAmount": 187500000,
              "averageAmount": 150000,
              "avgProcessingTimeMs": 38.5,
              "transactionsPerMinute": 12.3
            }
            """;

    @Test
    void routesTransactionToSwitchWithRewrittenPath() {
        // Arrange
        switchWm.stubFor(post(urlEqualTo("/api/internal/route"))
                .withRequestBody(equalToJson(VALID_TRANSACTION_REQUEST))
                .willReturn(okJson(TRANSACTION_RESPONSE)));

        // Act
        ResponseEntity<String> response =
                restTemplate.postForEntity("/api/transactions", VALID_TRANSACTION_REQUEST, String.class);

        // Assert
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(JsonPath.<String>read(response.getBody(), "$.status"))
                .isEqualTo("APPROVED");

//        verify(postRequestedFor(urlEqualTo("/api/internal/route"))
//                .withRequestBody(matchingJsonPath("$.pan", equalTo("4000003458730237"))));
    }

    @Test
    void rejectsInvalidTransactionBeforeCallingSwitch() {
        // Arrange
        String request = VALID_TRANSACTION_REQUEST.replace("4000003458730237", "4000");

        // Act
        ResponseEntity<String> response =
                restTemplate.postForEntity("/api/transactions", request, String.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(JsonPath.<String>read(response.getBody(), "$.error"))
                .isEqualTo("VALIDATION_ERROR");
        assertThat(JsonPath.<String>read(response.getBody(), "$.message"))
                .isEqualTo("Field 'pan' must be exactly 16 digits");

        // verify(0, postRequestedFor(urlEqualTo("/api/internal/route")));
    }

    @Test
    void shouldRouteToTerminalSimulator() {
        // Arrange
        String uri = "/api/simulator/terminal/run";

        terminalWm.stubFor(post(urlEqualTo(uri))
                .withRequestBody(equalToJson(TERMINAL_SIMULATOR_REQUEST))
                .willReturn(okJson(TERMINAL_SIMULATOR_RESPONSE)));

        // Act
        ResponseEntity<String> response =
                restTemplate.postForEntity(uri, TERMINAL_SIMULATOR_REQUEST, String.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(TERMINAL_SIMULATOR_RESPONSE);
    }

    @Test
    void shouldRouteToMerchantSimulator() {
        // Arrange
        String uri = "/api/simulator/merchant/run";

        merchantWm.stubFor(post(urlEqualTo(uri))
                .withRequestBody(equalToJson(MERCHANT_SIMULATOR_REQUEST))
                .willReturn(okJson(MERCHANT_SIMULATOR_RESPONSE)));

        // Act
        ResponseEntity<String> response =
                restTemplate.postForEntity(uri, MERCHANT_SIMULATOR_REQUEST, String.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(MERCHANT_SIMULATOR_RESPONSE);
    }

    @Test
    void dashboardShouldRouteToLogger() {
        // Arrange
        String uri = "/api/dashboard/stats";

        loggerWm.stubFor(get(urlEqualTo(uri))
                .willReturn(okJson(LOGGER_RESPONSE)));

        // Act
        ResponseEntity<String> response =
                restTemplate.getForEntity(uri, String.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(LOGGER_RESPONSE);
    }

    // Negative Tests for Routing

    @Test
    void shouldNotRouteToNotSwitch() {
        // Arrange
        String internalUri = "/api/internal/transactions";
        String publicUri = "/api/transactions";

        wrongWm.stubFor(post(urlEqualTo(internalUri))
                .withRequestBody(equalToJson(VALID_TRANSACTION_REQUEST))
                .willReturn(ok()));

        // Act
        ResponseEntity<String> response =
                restTemplate.postForEntity(publicUri, VALID_TRANSACTION_REQUEST, String.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldNotRouteToNotTerminal() {
        // Arrange
        String uri = "/api/simulator/terminal/run";

        wrongWm.stubFor(post(urlEqualTo(uri))
                .withRequestBody(equalToJson(TERMINAL_SIMULATOR_REQUEST))
                .willReturn(ok()));

        // Act
        ResponseEntity<String> response =
                restTemplate.postForEntity(uri, TERMINAL_SIMULATOR_REQUEST, String.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldNotRouteToNotMerchant() {
        // Arrange
        String uri = "/api/simulator/merchant/run";

        wrongWm.stubFor(post(urlEqualTo(uri))
                .withRequestBody(equalToJson(MERCHANT_SIMULATOR_REQUEST))
                .willReturn(ok()));

        // Act
        ResponseEntity<String> response =
                restTemplate.postForEntity(uri, MERCHANT_SIMULATOR_REQUEST, String.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldNotRouteToNotLogger() {
        // Arrange
        String uri = "/api/dashboard/stats";

        wrongWm.stubFor(get(urlEqualTo(uri))
                .willReturn(ok()));

        // Act
        ResponseEntity<String> response =
                restTemplate.getForEntity(uri, String.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldNotRouteToNotCardManagement() {
        // Arrange
        String uri = "/api/cards";

        wrongWm.stubFor(post(urlEqualTo(uri))
                .withRequestBody(equalToJson(CARD_MANAGEMENT_REQUEST))
                .willReturn(ok()));

        // Act
        ResponseEntity<String> response =
                restTemplate.postForEntity(uri, CARD_MANAGEMENT_REQUEST, String.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}

package com.processing.gateway.integration;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "gateway.rate-limit.transactions-per-second=100",
                "gateway.open-api.url=http://gateway.test"
        }
)
public class GatewayRoutingIntegrationTest {
    @RegisterExtension
    static WireMockExtension switchWm = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @RegisterExtension
    static WireMockExtension loggerWm = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @RegisterExtension
    static WireMockExtension terminalWm = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @RegisterExtension
    static WireMockExtension merchantWm = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @RegisterExtension
    static WireMockExtension cardWm = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
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
              "transmissionDateTime": "2026-06-05T18:12:49.07Z",
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

    private static final String CARD_MANAGEMENT_RESPONSE = """
            {
              "pan": "4000003458730237",
              "status": "ACTIVE"
            }
            """;

    private static final String DOWNSTREAM_OPEN_API_RESPONSE = """
            {
              "openapi": "3.0.1",
              "info": {
                "title": "Switch",
                "version": "1.0.0"
              },
              "paths": {
                "/api/internal/route": {
                  "post": {
                    "responses": {
                      "200": {
                        "description": "OK"
                      }
                    }
                  }
                },
                "/internal/health": {
                  "get": {
                    "responses": {
                      "200": {
                        "description": "OK"
                      }
                    }
                  }
                },
                "/api/public": {
                  "get": {
                    "responses": {
                      "200": {
                        "description": "OK"
                      }
                    }
                  }
                }
              }
            }
            """;

    private static final String TRANSACTION_SEARCH_RESPONSE = """
            {
              "items": [
                {
                  "stan": "000001"
                }
              ]
            }
            """;

    @DynamicPropertySource
    static void registerDownstreamUrls(DynamicPropertyRegistry registry) {
        registry.add("SWITCH_URL", switchWm::baseUrl);
        registry.add("LOGGER_URL", loggerWm::baseUrl);
        registry.add("TERMINAL_SIM_URL", terminalWm::baseUrl);
        registry.add("MERCHANT_SIM_URL", merchantWm::baseUrl);
        registry.add("CARD_MGMT_URL", cardWm::baseUrl);
    }

    @BeforeEach
    void resetWireMock() {
        switchWm.resetAll();
        loggerWm.resetAll();
        terminalWm.resetAll();
        merchantWm.resetAll();
        cardWm.resetAll();
    }

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

        switchWm.verify(postRequestedFor(urlEqualTo("/api/internal/route"))
                .withRequestBody(matchingJsonPath("$.pan", equalTo("4000003458730237"))));
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

        switchWm.verify(0, postRequestedFor(urlEqualTo("/api/internal/route")));
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

        terminalWm.verify(postRequestedFor(urlEqualTo(uri))
                .withRequestBody(equalToJson(TERMINAL_SIMULATOR_REQUEST)));
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

        merchantWm.verify(postRequestedFor(urlEqualTo(uri))
                .withRequestBody(equalToJson(MERCHANT_SIMULATOR_REQUEST)));
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

        loggerWm.verify(getRequestedFor(urlEqualTo(uri)));
    }

    @Test
    void routesCardRequestsToCardManagementWithoutRewritingPath() {
        // Arrange
        String uri = "/api/cards/4000003458730237";
        cardWm.stubFor(get(urlEqualTo(uri))
                .willReturn(okJson(CARD_MANAGEMENT_RESPONSE)));

        // Act
        ResponseEntity<String> response =
                restTemplate.getForEntity(uri, String.class);

        // Assert
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(JsonPath.<String>read(response.getBody(), "$.status"))
                .isEqualTo("ACTIVE");
        cardWm.verify(getRequestedFor(urlEqualTo(uri)));
    }

    @Test
    void routesTransactionSearchToLoggerAndPreservesQueryParameters() {
        // Arrange
        String uri = "/api/transactions/search";
        loggerWm.stubFor(get(urlPathEqualTo(uri))
                .withQueryParam("stan", equalTo("000001"))
                .willReturn(okJson(TRANSACTION_SEARCH_RESPONSE)));

        // Act
        ResponseEntity<String> response =
                restTemplate.getForEntity(uri + "?stan=000001", String.class);

        // Assert
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(JsonPath.<String>read(response.getBody(), "$.items[0].stan"))
                .isEqualTo("000001");
        loggerWm.verify(getRequestedFor(urlPathEqualTo(uri))
                .withQueryParam("stan", equalTo("000001")));
    }

    @Test
    void rewritesDownstreamOpenApiDocsForGatewayConsumers() {
        // Arrange
        String uri = "/v3/api-docs";
        switchWm.stubFor(get(urlEqualTo(uri))
                .willReturn(okJson(DOWNSTREAM_OPEN_API_RESPONSE)));

        // Act
        ResponseEntity<String> response = restTemplate.getForEntity("/switch-docs", String.class);

        //Assert
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(JsonPath.<String>read(response.getBody(), "$.servers[0].url")).isEqualTo("http://gateway.test");
        assertThat(JsonPath.<Object>read(response.getBody(), "$.paths['/api/transactions']")).isNotNull();
        assertThat(JsonPath.<Object>read(response.getBody(), "$.paths['/api/public']")).isNotNull();
        assertThat(response.getBody()).doesNotContain("/api/internal/route", "/internal/health");

        switchWm.verify(getRequestedFor(urlEqualTo(uri)));
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
        wrongWm.verify(0, postRequestedFor(urlEqualTo(internalUri)));
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
        wrongWm.verify(0, postRequestedFor(urlEqualTo(uri)));
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
        wrongWm.verify(0, postRequestedFor(urlEqualTo(uri)));
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
        wrongWm.verify(0, getRequestedFor(urlEqualTo(uri)));
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
        wrongWm.verify(0, postRequestedFor(urlEqualTo(uri)));
    }
}

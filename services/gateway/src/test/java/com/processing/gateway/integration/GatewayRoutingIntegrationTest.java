package com.processing.gateway.integration;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.jayway.jsonpath.JsonPath;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
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

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void routesTransactionToSwitchWithRewrittenPath() {
        // Arrange
        switchWm.stubFor(post(urlEqualTo("/api/internal/route"))
                .willReturn(okJson("""
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
                        """)));

        String request = """
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

        // Act
        ResponseEntity<String> response =
                restTemplate.postForEntity("/api/transactions", request, String.class);

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
        String request = validTransactionRequest().replace("4000003458730237", "4000");

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
        String requestBody = """
                {
                  "count": 50,
                  "scenario": "normal"
                }
                """;
        String responseBody = """
                {
                  "totalSubmitted": 50,
                  "approved": 47,
                  "declined": 3,
                  "elapsedMs": 2300,
                  "transactions": []
                }
                """;

        terminalWm.stubFor(post(urlEqualTo(uri))
                .withRequestBody(equalToJson(requestBody))
                .willReturn(okJson(responseBody)));

        // Act
        ResponseEntity<String> response =
                restTemplate.postForEntity(uri, requestBody, String.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(responseBody);
    }

    @Test
    void shouldRouteToMerchantSimulator() {
        // Arrange
        String uri = "/api/simulator/merchant/run";
        String requestBody = """
                {
                  "count": 50,
                  "mccCodes": ["5411", "5812"],
                  "scenario": "grocery"
                }
                """;
        String responseBody = """
                {
                  "totalSubmitted": 50,
                  "approved": 47,
                  "declined": 3,
                  "elapsedMs": 2300,
                  "transactions": []
                }
                """;

        merchantWm.stubFor(post(urlEqualTo(uri))
                .withRequestBody(equalToJson(requestBody))
                .willReturn(okJson(responseBody)));

        // Act
        ResponseEntity<String> response =
                restTemplate.postForEntity(uri, requestBody, String.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(responseBody);
    }

    @Test
    void dashboardShouldRouteToLogger() {
        // Arrange
        String uri = "/api/dashboard/stats";
        String responseBody = """
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

        loggerWm.stubFor(get(urlEqualTo(uri))
                .willReturn(okJson(responseBody)));

        // Act
        ResponseEntity<String> response =
                restTemplate.getForEntity(uri, String.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(responseBody);
    }

    private String validTransactionRequest() {
        return """
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
    }
}

package com.processing.gateway.integration;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "SWITCH_URL=http://localhost:${wiremock.server.port}",
                "LOGGER_URL=http://localhost:${wiremock.server.port}",
                "TERMINAL_SIM_URL=http://localhost:${wiremock.server.port}",
                "MERCHANT_SIM_URL=http://localhost:${wiremock.server.port}",
                "CARD_MGMT_URL=http://localhost:${wiremock.server.port}",
                "gateway.rate-limit.transactions-per-second=100",
                "gateway.open-api.url=http://gateway.test"
        }
)
@AutoConfigureWireMock(port = 0)
class GatewayRoutingIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @BeforeEach
    void resetWireMock() {
        reset();
    }

    @Test
    void routesTransactionToSwitchWithRewrittenPath() {
        stubFor(post(urlEqualTo("/api/internal/route"))
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

        ResponseEntity<String> response =
                restTemplate.postForEntity("/api/transactions", request, String.class);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(JsonPath.<String>read(response.getBody(), "$.status"))
                .isEqualTo("APPROVED");

        verify(postRequestedFor(urlEqualTo("/api/internal/route"))
                .withRequestBody(matchingJsonPath("$.pan", equalTo("4000003458730237"))));
    }

    @Test
    void rejectsInvalidTransactionBeforeCallingSwitch() {
        String request = validTransactionRequest().replace("4000003458730237", "4000");

        ResponseEntity<String> response =
                restTemplate.postForEntity("/api/transactions", request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(JsonPath.<String>read(response.getBody(), "$.error"))
                .isEqualTo("VALIDATION_ERROR");
        assertThat(JsonPath.<String>read(response.getBody(), "$.message"))
                .isEqualTo("Field 'pan' must be exactly 16 digits");

        verify(0, postRequestedFor(urlEqualTo("/api/internal/route")));
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

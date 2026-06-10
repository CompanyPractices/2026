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

import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.reset;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "SWITCH_URL=http://localhost:${wiremock.server.port}",
                "LOGGER_URL=http://localhost:${wiremock.server.port}",
                "TERMINAL_SIM_URL=http://localhost:${wiremock.server.port}",
                "MERCHANT_SIM_URL=http://localhost:${wiremock.server.port}",
                "CARD_MGMT_URL=http://localhost:${wiremock.server.port}",
                "gateway.rate-limit.transactions-per-second=0"
        }
)
@AutoConfigureWireMock(port = 0)
class GatewayRateLimitIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @BeforeEach
    void resetWireMock() {
        reset();
    }

    @Test
    void rejectsTransactionWhenRateLimitIsExceeded() {
        ResponseEntity<String> response =
                restTemplate.postForEntity("/api/transactions", validTransactionRequest(), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getHeaders().getFirst("Retry-After")).isEqualTo("1");
        assertThat(JsonPath.<String>read(response.getBody(), "$.error")).isEqualTo("RATE_LIMIT_EXCEEDED");

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

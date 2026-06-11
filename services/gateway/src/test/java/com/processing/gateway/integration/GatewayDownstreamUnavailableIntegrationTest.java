package com.processing.gateway.integration;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.cloud.gateway.mvc.routes[0].id=authorization",
                "spring.cloud.gateway.mvc.routes[0].uri=http://localhost:1",
                "spring.cloud.gateway.mvc.routes[0].metadata.serviceName=authorization",
                "spring.cloud.gateway.mvc.routes[0].predicates[0]=Path=/api/internal/authorize",
                "spring.cloud.gateway.mvc.routes[0].predicates[1]=Method=POST"
        }
)
class GatewayDownstreamUnavailableIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void returnsServiceUnavailableWhenAuthorizationServiceIsDown() {
        String uri = "/api/internal/authorize";
        ResponseEntity<String> response =
                restTemplate.postForEntity(uri, validAuthorizationRequest(), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(JsonPath.<String>read(response.getBody(), "$.error"))
                .isEqualTo("SERVICE_UNAVAILABLE");
        assertThat(JsonPath.<String>read(response.getBody(), "$.message"))
                .isEqualTo("Authorization service is temporarily unavailable");
        assertThat(JsonPath.<String>read(response.getBody(), "$.serviceName"))
                .isEqualTo("authorization");
    }

    private String validAuthorizationRequest() {
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

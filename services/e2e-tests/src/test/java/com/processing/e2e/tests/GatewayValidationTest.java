package com.processing.e2e.tests;

import com.fasterxml.jackson.databind.JsonNode;
import com.processing.e2e.E2EBaseTest;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.time.Instant;
import java.util.List;

/*
 * TC-11 - Checks that gateway rejects invalid queries with HTTP 400
 */
public class GatewayValidationTest extends E2EBaseTest {
    private static final String VALID_TRANSACTION_REQUEST = """
            {
              "mti": "0100",
              "stan": "000001",
              "pan": "4000003458730237",
              "processingCode": "000000",
              "amount": 100,
              "currencyCode": "643",
              "transmissionDateTime": "2026-06-01T10:30:00Z",
              "terminalId": "TERM001",
              "terminalType": "POS",
              "merchantId": "MERCH00000000001",
              "mcc": "5411",
              "acquirerId": "ACQ001"
            }
            """;

    @Test(description = "TC-11 - Gateway rejects invalid transaction requests")
    public void gatewayRejectsInvalidTransactionRequests() {
        for (InvalidRequestCase testCase : invalidRequestCases()) {
            JsonNode response = httpUtils.httpPost(GATEWAY_URL, "/api/transactions", testCase.body(), 400);

            Assert.assertEquals(response.path("error").asText(), "VALIDATION_ERROR",
                    testCase.name() + ": $.error should be VALIDATION_ERROR");
            Assert.assertFalse(response.path("message").asText().isBlank(),
                    testCase.name() + ": $.message should explain validation failure");
            Assert.assertEquals(response.path("serviceName").asText(), "gateway",
                    testCase.name() + ": $.serviceName should be gateway");
            Assert.assertFalse(response.path("timestamp").asText().isBlank(),
                    testCase.name() + ": $.timestamp should be present");
            Assert.assertNotNull(Instant.parse(response.path("timestamp").asText()),
                    testCase.name() + ": $.timestamp should be an ISO-8601 instant");
        }
    }

    private List<InvalidRequestCase> invalidRequestCases() {
        return List.of(
                new InvalidRequestCase("empty body", "{}"),
                new InvalidRequestCase("missing pan", """
                        {
                          "mti": "0100",
                          "stan": "000001",
                          "processingCode": "000000",
                          "amount": 100,
                          "currencyCode": "643",
                          "transmissionDateTime": "2026-06-01T10:30:00Z",
                          "terminalId": "TERM001",
                          "terminalType": "POS",
                          "merchantId": "MERCH00000000001",
                          "mcc": "5411",
                          "acquirerId": "ACQ001"
                        }
                        """),
                new InvalidRequestCase("pan is too short",
                        VALID_TRANSACTION_REQUEST.replace("4000003458730237", "123")),
                new InvalidRequestCase("amount is negative",
                        VALID_TRANSACTION_REQUEST.replace("\"amount\": 100", "\"amount\": -100"))
        );
    }

    private record InvalidRequestCase(String name, String body) {
    }
}

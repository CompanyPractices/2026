package com.processing.e2e.tests;

import com.fasterxml.jackson.databind.JsonNode;
import com.processing.e2e.E2EBaseTest;
import com.processing.e2e.utility.DBUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.sql.SQLException;
import java.util.List;

/*
 * TC-11 - Checks that gateway rejects invalid queries with HTTP 400
 */
public class GatewayValidationTest extends E2EBaseTest {
    private final DBUtils dbUtils = new DBUtils();

    private static final String TRANSACTIONS_COUNT_SQL = "SELECT COUNT(*) FROM transactions";

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
    public void gatewayRejectsInvalidTransactionRequests() throws SQLException {
        long transactionsBefore = dbUtils.queryLong(TRANSACTIONS_COUNT_SQL);

        for (InvalidRequestCase testCase : invalidRequestCases()) {
            JsonNode response = httpUtils.httpPost(GATEWAY_URL, "/api/transactions", testCase.body(), 400);

            Assert.assertEquals(response.path("error").asText(), "VALIDATION_ERROR",
                    testCase.name() + ": $.error should be VALIDATION_ERROR");
            Assert.assertFalse(response.path("message").asText().isBlank(),
                    testCase.name() + ": $.message should explain validation failure");
        }

        long transactionsAfter = dbUtils.queryLong(TRANSACTIONS_COUNT_SQL);
        Assert.assertEquals(transactionsAfter, transactionsBefore,
                "Invalid transaction requests must not create transaction records");
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

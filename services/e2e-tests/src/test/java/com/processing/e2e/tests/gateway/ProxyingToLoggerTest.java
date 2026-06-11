package com.processing.e2e.tests.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.processing.e2e.E2EBaseTest;
import com.processing.e2e.utility.DBUtils;
import org.testng.annotations.Test;

import java.sql.SQLException;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/*
 * TC-13
 */
public class ProxyingToLoggerTest extends E2EBaseTest {
    private final DBUtils dbUtils = new DBUtils();

    private JsonNode gatewayResponse;

    @Test(priority = 1300)
    void preparationCreateTransactions() {
        String cardsRequestBody = """
                {
                  "bin": "400000",
                  "cardholderName": "PROXY TEST",
                  "currencyCode": "643",
                  "dailyLimit": 1000000,
                  "monthlyLimit": 10000000,
                  "initialBalance": 5000000
                }
                """;

        JsonNode cardsResponse = httpUtils.httpPost(GATEWAY_URL, "/api/cards", cardsRequestBody, 201);

        String transactionsRequestBody = """
                {
                  "mti": "0100",
                  "stan": "000001",
                  "pan": %s,
                  "processingCode": "000000",
                  "amount": 150000,
                  "currencyCode": "643",
                  "transmissionDateTime": "2026-06-01T10:30:00Z",
                  "terminalId": "TERM0010",
                  "terminalType": "POS",
                  "merchantId": "MERCH1234567890",
                  "mcc": "5411",
                  "acquirerId": "ACQ001"
                }
                """.formatted(cardsResponse.path("pan").asText());

        httpUtils.httpPost(GATEWAY_URL, "/api/transactions", transactionsRequestBody, 200);
    }

    @Test(priority = 1301)
    void getTransactionsSearchThroughGatewayShouldReturnOk() {
        gatewayResponse = httpUtils.httpGet(GATEWAY_URL, "/api/transactions/search?limit=5", 200);

        assertTrue(gatewayResponse.path("total").asInt() > 0);
        assertTrue(gatewayResponse.path("transactions").isArray());
        assertTrue(gatewayResponse.path("transactions").size() <= 5);
    }

    @Test(priority = 1302)
    void getTransactionsSearchDirectlyShouldMatch() {
        JsonNode response = httpUtils.httpGet(LOGGER_URL, "/api/transactions/search?limit=5", 200);

        assertEquals(response, gatewayResponse);
    }

    @Test(priority = 1303)
    void getDashboardStatsShouldReturnOk() {
        JsonNode response = httpUtils.httpGet(GATEWAY_URL, "/api/dashboard/stats", 200);

        assertTrue(response.path("totalTransactions").asInt() > 0);
        assertTrue(response.path("approvedCount").asInt() >= 0);
        assertTrue(response.path("declinedCount").asInt() >= 0);
        assertEquals(response.path("totalTransactions").asInt(),
                response.path("approvedCount").asInt()
                        + response.path("declinedCount").asInt());
    }

    @Test(priority = 1304)
    void getDashboardRecentLimit10ShouldReturnNoMoreThan10Elements() {
        JsonNode response = httpUtils.httpGet(GATEWAY_URL, "/api/dashboard/recent?limit=5", 200);

        assertTrue(response.isArray());
        assertTrue(response.size() <= 10);

        for (JsonNode transaction : response) {
            assertTrue(transaction.has("pan"));
            assertTrue(transaction.has("status"));
            assertTrue(transaction.has("amount"));
        }
    }

    @Test(priority = 1305)
    void getTransactionsCountFromDbShouldMatchStats() throws SQLException {
        Long dbCount = dbUtils.queryLong("SELECT COUNT(*) FROM transactions");
        JsonNode httpResponse = httpUtils.httpGet(GATEWAY_URL, "/api/dashboard/stats", 200);

        assertEquals(dbCount, httpResponse.path("totalTransactions").asInt());
    }
}

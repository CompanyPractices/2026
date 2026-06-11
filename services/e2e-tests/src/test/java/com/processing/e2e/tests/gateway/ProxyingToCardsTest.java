package com.processing.e2e.tests.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.processing.e2e.E2EBaseTest;
import com.processing.e2e.utility.DBUtils;
import org.testng.annotations.Test;

import java.sql.SQLException;

import static org.testng.Assert.*;

/*
 * TC-12 - Checks if gateway proxying requests to Card Management Service correctly.
 */
public class ProxyingToCardsTest extends E2EBaseTest {
    private final DBUtils dbUtils = new DBUtils();

    private record ResponseFieldsToCompare(
            JsonNode id,
            JsonNode pan,
            JsonNode status,
            JsonNode balance
    ) {}

    private ResponseFieldsToCompare gatewayResponse;
    private ResponseFieldsToCompare directResponse;

    private JsonNode directResponseJson;
    private String pan;

    @Test(priority = 1201)
    void postApiCardsShouldReturn201() {
        String requestBody = """
                {
                    "bin": "400000",
                    "cardholderName": "PROXY TEST",
                    "currencyCode": "643",
                    "dailyLimit": 1000000,
                    "monthlyLimit": 10000000,
                    "initialBalance": 5000000
                }
                """;

        JsonNode response = httpUtils.httpPost(GATEWAY_URL, "/api/cards", requestBody, 201);

        gatewayResponse = new ResponseFieldsToCompare(
                response.path("id"),
                response.path("pan"),
                response.path("status"),
                response.path("availableBalance")
        );

        pan = gatewayResponse.pan().asText();
    }

    @Test(priority = 1202)
    void getCardByPanDirectlyShouldReturn200() {
        JsonNode response = httpUtils.httpGet(CARD_MGMT_URL, "/api/cards/" + pan, 200);

        directResponse = new ResponseFieldsToCompare(
                response.path("id"),
                response.path("pan"),
                response.path("status"),
                response.path("availableBalance")
        );

        directResponseJson = response;

        assertEquals(response.path("cardholderName").asText(), "PROXY TEST");
        assertEquals(response.path("availableBalance").asText(), "5000000");
    }

    @Test(priority = 1203)
    void compareGatewayAndDirectResponsesShouldMatch() {
        assertEquals(gatewayResponse, directResponse);
    }

    @Test(priority = 1204)
    void getCardByPanThroughGatewayShouldMatchDirect() {
        JsonNode response = httpUtils.httpGet(GATEWAY_URL, "/api/cards/" + pan, 200);

        assertEquals((Object) response, (Object) directResponseJson);
    }

    @Test(priority = 1205)
    void getCardsLimit5ShouldReturnArray() {
        JsonNode response = httpUtils.httpGet(GATEWAY_URL, "/api/cards?limit=5", 200);

        assertTrue(response.path("cards").isArray());
        assertTrue(response.path("total").asInt() > 1);
    }

    @Test(priority = 1206)
    void checkCreatedCardInDbShouldExist() throws SQLException {
        String query = "SELECT * FROM cards WHERE pan = ?";

        String result = dbUtils.queryString(query, pan);

        assertFalse(result.isEmpty());
    }
}

package com.processing.e2e.tests.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.processing.e2e.E2EBaseTest;
import com.processing.e2e.utility.DBUtils;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import java.sql.SQLException;

/*
 * TC-12 - Checks if gateway proxying requests to Card Management Service correctly.
 */
public class ProxyingToCardsTest extends E2EBaseTest {

    private final SoftAssert soft = new SoftAssert();
    private final DBUtils dbUtils = new DBUtils();

    private record ResponseFieldsToCompare(
            String id,
            String pan,
            String status,
            String balance
    ) {}

    private ResponseFieldsToCompare gatewayResponse;
    private ResponseFieldsToCompare directResponse;

    private String directResponseJson;
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
                response.path("id").asText(),
                response.path("pan").asText(),
                response.path("status").asText(),
                response.path("availableBalance").asText()
        );

        pan = gatewayResponse.pan();
    }

    @Test(priority = 1202)
    void getCardByPanDirectlyShouldReturn200() {
        JsonNode response = httpUtils.httpGet(CARD_MGMT_URL, "/api/cards/" + pan, 200);

        directResponse = new ResponseFieldsToCompare(
                response.path("id").asText(),
                response.path("pan").asText(),
                response.path("status").asText(),
                response.path("availableBalance").asText()
        );

        directResponseJson = response.asText();

        soft.assertEquals(response.path("cardholderName"), "PROXY TEST");
        soft.assertEquals(response.path("availableBalance"), "5000000");
    }

    @Test(priority = 1203)
    void compareGatewayAndDirectResponsesShouldMatch() {
        soft.assertEquals(gatewayResponse.id(), directResponse.id());
        soft.assertEquals(gatewayResponse.pan(), directResponse.pan());
        soft.assertEquals(gatewayResponse.status(), directResponse.status());
        soft.assertEquals(gatewayResponse.balance(), directResponse.balance());
    }

    @Test(priority = 1204)
    void getCardByPanThroughGatewayShouldMatchDirect() {
        JsonNode response = httpUtils.httpGet(GATEWAY_URL, "/api/cards/" + pan, 200);

        soft.assertEquals(response.asText(), directResponseJson);
    }

    @Test(priority = 1205)
    void getCardsLimit5ShouldReturnArray() {
        JsonNode response = httpUtils.httpGet(GATEWAY_URL, "/api/cards?limit=5", 200);

        soft.assertEquals(response.path("cards").isArray(), true);
        soft.assertEquals(response.path("total").asInt() > 1, true);
    }

    @Test(priority = 1206)
    void checkCreatedCardInDbShouldExist() throws SQLException {
        String query = "SELECT * FROM cards WHERE pan = ?";

        String result = dbUtils.queryString(query, pan);

        soft.assertEquals(result.isEmpty(), false);
    }
}

package com.processing.e2e.tests;


import com.fasterxml.jackson.databind.JsonNode;
import com.processing.e2e.E2EBaseTest;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;


/**
 * TC-01 — Health-check of all services.
 * Goal: get HTTP 200 on /health of each service.
 * For JSON services also check $.status == "ok".
 * Dashboard — HTML, only statusCode is checked.
 * All checks in one method, SoftAssert.
 */
public class HealthCheckTest extends E2EBaseTest {


    @Test(description = "TC-01 — Health-check of all services")
    public void allServicesHealthCheck() {
        SoftAssert soft = new SoftAssert();


        JsonNode gateway = httpGet(GATEWAY_URL, "/health", 200);
        soft.assertEquals(gateway.path("status").asText(), "ok",
                "Gateway: $.status should be 'ok'");
        soft.assertEquals(gateway.path("service").asText(), "gateway",
                "Gateway: $.service does not match");


        JsonNode cardMgmt = httpGet(CARD_MGMT_URL, "/health", 200);
        soft.assertEquals(cardMgmt.path("status").asText(), "ok",
                "Card Management: $.status should be 'ok'");
        soft.assertEquals(cardMgmt.path("service").asText(), "card-management",
                "Card Management: $.service does not match");


        JsonNode switchSvc = httpGet(SWITCH_URL, "/health", 200);
        soft.assertEquals(switchSvc.path("status").asText(), "ok",
                "Switch: $.status should be 'ok'");
        soft.assertEquals(switchSvc.path("service").asText(), "switch",
                "Switch: $.service does not match");


        JsonNode auth = httpGet(AUTH_URL, "/health", 200);
        soft.assertEquals(auth.path("status").asText(), "ok",
                "Authorization: $.status should be 'ok'");
        soft.assertEquals(auth.path("service").asText(), "authorization",
                "Authorization: $.service does not match");


        JsonNode terminal = httpGet(TERMINAL_SIM_URL, "/health", 200);
        soft.assertEquals(terminal.path("status").asText(), "ok",
                "Terminal Simulator: $.status should be 'ok'");
        soft.assertEquals(terminal.path("service").asText(), "terminal-simulator",
                "Terminal Simulator: $.service does not match");


        JsonNode merchant = httpGet(MERCHANT_SIM_URL, "/health", 200);
        soft.assertEquals(merchant.path("status").asText(), "ok",
                "Merchant Acquirer: $.status should be 'ok'");
        soft.assertEquals(merchant.path("service").asText(), "merchant-acquirer-simulator",
                "Merchant Acquirer: $.service does not match");


        JsonNode logger = httpGet(LOGGER_URL, "/health", 200);
        soft.assertEquals(logger.path("status").asText(), "ok",
                "Transaction Logger: $.status should be 'ok'");
        soft.assertEquals(logger.path("service").asText(), "transaction-logger",
                "Transaction Logger: $.service does not match");


        soft.assertAll();
    }
}

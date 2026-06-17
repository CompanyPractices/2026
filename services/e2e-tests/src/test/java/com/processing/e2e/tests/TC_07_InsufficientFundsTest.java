package com.processing.e2e.tests;

import com.fasterxml.jackson.databind.JsonNode;
import com.processing.e2e.E2EBaseTest;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import java.sql.SQLException;

import static org.testng.Assert.assertTrue;

/**
 * TC-07 - Decline: Insufficient Funds (responseCode 51).
 * Goal: check reject based on insufficient funds.
 * Available balance in database must not change
 */
public class TC_07_InsufficientFundsTest extends E2EBaseTest {
    @Test(description = "TC-07: Decline - Insufficient Funds")
    public void declineInsufficientFunds() throws SQLException {
        SoftAssert soft = new SoftAssert();

        String createCardBody = """
                {
                  "bin": "400000",
                  "cardholderName": "JHON GOLT",
                  "currencyCode": "643",
                  "dailyLimit": 15000000,
                  "monthlyLimit": 300000000,
                  "initialBalance": 1000
                }
                """;
        JsonNode card = httpPostRaw(GATEWAY_URL, "/api/cards", createCardBody, 201);
        String pan = card.path("pan").asText();

        long balanceBefore = db.queryLong(
                "SELECT available_balance FROM cards WHERE pan = ?", pan);
        soft.assertEquals(balanceBefore, 1000L, "Initial balance mismatch");

        String stan = "100007";
        String authRequest = """
                {
                  "mti": "0100",
                  "stan": "%s",
                  "pan": "%s",
                  "processingCode": "000000",
                  "amount": 5000,
                  "currencyCode": "643",
                  "transmissionDateTime": "2026-06-05T18:12:49.070",
                  "terminalId": "TERM0001",
                  "merchantId": "MERCH0000000001",
                  "mcc": "5411",
                  "acquirerId": "ACQ001",
                  "issuerId": "I001"
                }
                """.formatted(stan, pan);

        JsonNode response = httpPostRaw(GATEWAY_URL, "/api/transactions", authRequest, 200);

        soft.assertEquals(response.path("status").asText(), "DECLINED", "Status should be DECLINED");
        soft.assertEquals(response.path("responseCode").asText(), "51", "responseCode should be 51");
        String declineReason = response.path("declineReason").asText("");
        assertTrue(declineReason.toUpperCase().contains("INSUFFICIENT"),
                "declineReason should contain INSUFFICIENT_FUNDS, was: " + declineReason);
        soft.assertTrue(response.path("rrn").isNull() || response.path("rrn").asText().isEmpty(),
                "rrn should be null/empty");
        soft.assertTrue(response.path("authCode").isNull() || response.path("authCode").asText().isEmpty(),
                "authCode should be null/empty");

        long balanceAfter = db.queryLong(
                "SELECT available_balance FROM cards WHERE pan = ?", pan);
        soft.assertEquals(balanceAfter, balanceBefore,
                "Balance must not change on decline");

        String dbDeclineReason = db.queryString(
                "SELECT decline_reason FROM transactions WHERE stan = ? AND status = 'DECLINED'", stan);
        soft.assertTrue(dbDeclineReason.toUpperCase().contains("INSUFFICIENT"),
                "DB decline_reason should contain INSUFFICIENT_FUNDS, was: " + dbDeclineReason);

        soft.assertAll();
    }
}

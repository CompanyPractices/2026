package com.processing.e2e.tests;

import com.fasterxml.jackson.databind.JsonNode;
import com.processing.e2e.E2EBaseTest;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;
import java.sql.SQLException;

/**
 * TC-10 — Decline: card expired.
 * Goal: check reject based on cards date expired.
 * All checks in one method, SoftAssert.
 */
public class TC_10_CardExpiredTest extends E2EBaseTest {
    @Test(description = "TC-10 — Decline: card expired (responseCode 54)")
    public void DeclineCardExpired() throws SQLException {
        SoftAssert soft = new SoftAssert();

        // Create card ---
        String createCardRequest = """
                {
                    "bin": 400000,
                    "cardholderName": "IVAN IVANOVV",
                    "currencyCode": 643,
                    "dailyLimit": 15000000,
                    "monthlyLimit": 300000000,
                    "initialBalance": 1000000000
                }
                """;

        JsonNode postCard = httpPostRaw(CARD_MGMT_URL, "/api/cards", createCardRequest, 201);
        soft.assertNotNull(postCard, "Cards POST response should not be null");
        soft.assertFalse(postCard.isEmpty(), "Cards POST response should not be empty");
        String pan = postCard.path("pan").asText();
        soft.assertNotNull(pan, "PAN should not be null after card creation");
        soft.assertFalse(pan.isEmpty(), "PAN should not be empty after card creation");

        // DB check — card is ACTIVE
        String cardStatusActive = db.queryString(
                "SELECT status FROM cards WHERE pan = '" + pan + "'");
        soft.assertEquals(cardStatusActive, "ACTIVE",
                "DB: card status should be 'ACTIVE' after creation, but was: " +
                        cardStatusActive);
        // Expire the card
        db.executeUpdate(
                "UPDATE cards SET expiry_date = '0120' WHERE pan = '" + pan + "'");

        // DB check — card is EXPIRED
        String cardExpiryDate = db.queryString(
                "SELECT expiry_date FROM cards WHERE pan = '" + pan + "'");
        soft.assertEquals(cardExpiryDate, "0120",
                "DB: card expiry date should be '0120' after patch, but was: " +
                        cardExpiryDate);

        // Send transaction with expired card
        String transactionRequest = """
                {
                "mti": "0100",
                "stan": "000001",
                "pan": "%s",
                "processingCode": "000000",
                "amount": 150000,
                "currencyCode": "643",
                "transmissionDateTime": "2026-06-01T10:30:00Z",
                "terminalId": "TERM0001",
                "terminalType": "POS",
                "merchantId": "MERCH1234567890",
                "mcc": "5411",
                "acquirerId": "ACQ001"
                }
                """.formatted(pan);
        JsonNode postTransaction = httpPostRaw(GATEWAY_URL, "/api/transactions",
                transactionRequest, 200);

        soft.assertEquals(postTransaction.path("status").asText(), "DECLINED",
                "Transaction: $.status should be 'DECLINED'");
        soft.assertEquals(postTransaction.path("responseCode").asText(), "54",
                "Transaction: $.responseCode should be '54'");
        soft.assertTrue(
                postTransaction.path("declineReason").asText().contains("EXPIRED"),
                "Transaction: $.declineReason should contain 'EXPIRED', but was: " + postTransaction.path("declineReason").asText());

        // DB check — declined transaction record
        String dbDeclineReason = db.queryString(
                "SELECT decline_reason FROM transactions WHERE pan = '" + pan + "' AND status = 'DECLINED'");
        soft.assertTrue(
                dbDeclineReason != null && dbDeclineReason.contains("EXPIRED"),
                "DB: decline_reason should contain 'EXPIRED', but was: " + dbDeclineReason);
        soft.assertAll();
    }
}

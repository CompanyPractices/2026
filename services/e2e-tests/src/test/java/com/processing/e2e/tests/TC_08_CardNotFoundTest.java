package com.processing.e2e.tests;

import com.fasterxml.jackson.databind.JsonNode;
import com.processing.e2e.E2EBaseTest;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import java.sql.SQLException;

import static org.testng.Assert.assertTrue;

/**
 * TC-08 - Decline: Invalid PAN/Card not found (responseCode 14)
 * Goal: check reject CARD_NOT_FOUND for nonexistent PAN.
 */
public class TC_08_CardNotFoundTest extends E2EBaseTest {
    private static final String UNKNOWN_PAN = "4000009999999999";

    @Test(description = "TC-08: Decline: - Invalid PAN/Card not found")
    public void declinedInvalidPan() throws SQLException {
        SoftAssert soft = new SoftAssert();

        long countCards = db.queryLong("SELECT COUNT(*) FROM cards WHERE pan = ?", UNKNOWN_PAN);
        soft.assertEquals(countCards, 0L, "Card with pan " + UNKNOWN_PAN + "already exist");

        String authRequest = """
                {
                  "mti": "0100",
                  "stan": "100007",
                  "pan": "%s",
                  "processingCode": "000000",
                  "amount": 5000,
                  "currencyCode": "643",
                  "transmissionDateTime": "2026-06-05T18:12:49.07Z",
                  "terminalId": "TERM0001",
                  "merchantId": "MERCH0000000001",
                  "mcc": "5411",
                  "acquirerId": "ACQ001",
                  "issuerId": "I001"
                }
                """.formatted(UNKNOWN_PAN);

        JsonNode response = httpPostRaw(GATEWAY_URL, "/api/transactions", authRequest, 200);
        soft.assertEquals(response.path("status").asText(), "DECLINED", "Status should be DECLINED");
        soft.assertEquals(response.path("responseCode").asText(), "14", "responseCode should be 14");
        String declineReason = response.path("declineReason").asText("");
        soft.assertTrue(declineReason.toUpperCase().contains("CARD_NOT_FOUND")
                || declineReason.toLowerCase().contains("card not found"),
                "declineReason should contain CARD_NOT_FOUND or \"card not found\", was: " + declineReason);
        soft.assertTrue(response.path("rrn").isNull() || response.path("rrn").asText().isEmpty(),
                "rrn should be null/empty");
        soft.assertTrue(response.path("authCode").isNull() || response.path("authCode").asText().isEmpty(),
                "authCode should be null/empty");


        String dbDeclineReason = db.queryString(
                "SELECT decline_reason FROM transactions WHERE pan = ? AND status = 'DECLINED'", UNKNOWN_PAN
        );
        soft.assertTrue(dbDeclineReason.toUpperCase().contains("CARD_NOT_FOUND"),
                "DB decline_reason should contain CARD_NOT_FOUND, was: " + dbDeclineReason);
        soft.assertAll();
    }
}

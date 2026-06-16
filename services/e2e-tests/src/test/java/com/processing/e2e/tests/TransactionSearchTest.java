package com.processing.e2e.tests;

import com.fasterxml.jackson.databind.JsonNode;
import com.processing.common.dto.authorization.AuthorizationRequest;
import com.processing.common.dto.cardmanagement.CreateCardRequest;
import com.processing.e2e.E2EBaseTest;
import com.processing.e2e.utility.DBUtils;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 TC-19 — Transaction search with filters and pagination.
 Goal: verify search endpoint filters (status, pan, AND-logic) and pagination correctness.
 Each HTTP result is cross-checked against DB count via JDBC.
 Setup creates one card with 3 APPROVED + 1 DECLINED transactions.
 */

public class TransactionSearchTest extends E2EBaseTest {
    private final DBUtils dbUtils = new DBUtils();

    private String knownPan;

    @BeforeClass(alwaysRun = true)
    public void setUpTransactions() {
        knownPan = createCard();

        sendTransaction(knownPan, 50000L, "190001");
        sendTransaction(knownPan, 50000L, "190002");
        sendTransaction(knownPan, 50000L, "190003");
        sendTransaction(knownPan, 1000000L, "190004");
    }

    @Test(description = "TC-19 - Transaction search with filters and pagination")
    public void transactionSearchWithFilters() throws Exception {
        SoftAssert soft = new SoftAssert();

        JsonNode allTransactions = httpGet(GATEWAY_URL, "/api/transactions/search?limit=10&offset=0", 200);
        long total = allTransactions.path("total").asLong();
        soft.assertTrue(total > 0, "$.total should be > 0");
        soft.assertTrue(allTransactions.path("transactions").size() <= 10,
                "$.transactions size should not exceed limit=10");

        long dbTotal = dbUtils.queryLong("SELECT COUNT(*) FROM transactions");
        soft.assertEquals(total, dbTotal, "$.total does not match DB count");

        JsonNode approvedTransactions = httpGet(GATEWAY_URL, "/api/transactions/search?status=APPROVED", 200);
        long totalApproved = approvedTransactions.path("total").asLong();
        for (JsonNode transaction : approvedTransactions.path("transactions")) {
            soft.assertEquals(transaction.path("status").asText(), "APPROVED",
                    "$.status should be APPROVED");
        }
        long dbApproved = dbUtils.queryLong("SELECT COUNT(*) FROM transactions WHERE status = 'APPROVED'");
        soft.assertEquals(totalApproved, dbApproved, "$.total for APPROVED does not match DB count");

        JsonNode declinedTransactions = httpGet(GATEWAY_URL, "/api/transactions/search?status=DECLINED", 200);
        long totalDeclined = declinedTransactions.path("total").asLong();
        for (JsonNode transaction : declinedTransactions.path("transactions")) {
            soft.assertEquals(transaction.path("status").asText(), "DECLINED",
                    "$.status should be DECLINED");
        }
        long dbDeclined = dbUtils.queryLong("SELECT COUNT(*) FROM transactions WHERE status = 'DECLINED'");
        soft.assertEquals(totalDeclined, dbDeclined, "$.total for DECLINED does not match DB count");

        JsonNode panTransactions = httpGet(GATEWAY_URL, "/api/transactions/search?pan=" + knownPan, 200);
        long totalByPan = panTransactions.path("total").asLong();
        for(JsonNode transaction : panTransactions.path("transactions")) {
            soft.assertEquals(transaction.path("pan").asText(), knownPan,
                    "$.pan does not knownPan");
        }
        long dbTotalByPan = dbUtils.queryLong("SELECT COUNT(*) FROM transactions WHERE pan = ?", knownPan);
        soft.assertEquals(totalByPan, dbTotalByPan, "$.total for pan filter does not match DB count");

        JsonNode andTransactions = httpGet(GATEWAY_URL,
                "/api/transactions/search?status=APPROVED&pan=" + knownPan, 200);
        long totalAnd = andTransactions.path("total").asLong();
        for (JsonNode transaction : andTransactions.path("transactions")) {
            soft.assertEquals(transaction.path("status").asText(), "APPROVED",
                    "AND: $.status should be APPROVED");
            soft.assertEquals(transaction.path("pan").asText(), knownPan,
                    "AND: $.pan does not match knownPan");
        }
        long dbTotalAnd = dbUtils.queryLong("SELECT COUNT(*) FROM transactions WHERE status = 'APPROVED' AND pan = ?", knownPan);
        soft.assertEquals(totalAnd, dbTotalAnd, "$.total for AND filter does not match DB count");

        JsonNode firstPage = httpGet(GATEWAY_URL, "/api/transactions/search?limit=2&offset=0", 200);
        soft.assertEquals(firstPage.path("transactions").size(), 2,
                "Page 1: $.transaction.size should be 2");
        String id1= firstPage.path("transactions").get(0).path("id").asText();
        String id2 = firstPage.path("transactions").get(1).path("id").asText();

        JsonNode secondPage = httpGet(GATEWAY_URL, "/api/transactions/search?limit=2&offset=2", 200);
        soft.assertEquals(secondPage.path("transactions").size(), 2,
                "Page 2: $.transactions.size should be 2");
        String id3 = secondPage.path("transactions").get(0).path("id").asText();
        String id4 = secondPage.path("transactions").get(1).path("id").asText();

        soft.assertNotEquals(id3, id1,"Page 2 id3 duplicates page 1 id1");
        soft.assertNotEquals(id3, id2, "Page 2 id3 duplicates page 1 id2");
        soft.assertNotEquals(id4, id1,"Page 2 id4 duplicates page 1 id1");
        soft.assertNotEquals(id4, id2, "Page 2 id4 duplicates page 1 id2");

        soft.assertAll();
    }

    private String createCard() {
        CreateCardRequest cardRequest = new CreateCardRequest(
                "400000",
                "SEARCH TEST USER",
                "643",
                BigDecimal.valueOf(15000000L),
                BigDecimal.valueOf(300000000L),
                BigDecimal.valueOf(400000L)
        );

        return httpPost(GATEWAY_URL, "/api/cards", cardRequest, 201).path("pan").asText();
    }

    private void sendTransaction(String pan, long amount, String stan) {
        AuthorizationRequest request = AuthorizationRequest.builder()
                .mti("0100")
                .stan(stan)
                .pan(pan)
                .processingCode("000000")
                .amount(BigDecimal.valueOf(amount))
                .currencyCode("643")
                .transmissionDateTime(LocalDateTime.now().toString())
                .terminalId("TERM0001")
                .terminalType("POS")
                .merchantId("MERCH0000000001")
                .mcc("5411")
                .acquirerId("ACQ001")
                .build();

        httpPost(GATEWAY_URL, "/api/transactions", request, 200);
    }
}

package com.processing.e2e.tests;

import com.fasterxml.jackson.databind.JsonNode;
import com.processing.common.dto.authorization.AuthorizationRequest;
import com.processing.common.dto.cardmanagement.CreateCardRequest;
import com.processing.common.dto.transactionlogger.TransactionRequest;
import com.processing.common.dto.transactionlogger.TransactionStatus;
import com.processing.e2e.E2EBaseTest;
import com.processing.e2e.utility.DBUtils;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.testng.Assert.assertEquals;

public class TC_20_DashboardStatsTest extends E2EBaseTest {
    private static final String[] TEST_STANS = {"200001", "200002", "200003"};
    private static final String TEST_CARDHOLDER_NAME = "DASHBOARD TEST USER";
    private static final long APPROVED_AMOUNT_1 = 50_000L;
    private static final long APPROVED_AMOUNT_2 = 60_000L;
    private static final long DECLINED_AMOUNT = 10_000L;

    private final DBUtils dbUtils = new DBUtils();
    private String testPan;

    @BeforeClass(alwaysRun = true)
    public void setUpDashboardTransactions() throws Exception {
        deleteTestTransactions();
        deleteStaleTestCards();
        testPan = createCard(TEST_CARDHOLDER_NAME, 200_000L);

        sendTransaction(testPan, APPROVED_AMOUNT_1, TEST_STANS[0], "APPROVED");
        sendTransaction(testPan, APPROVED_AMOUNT_2, TEST_STANS[1], "APPROVED");
        logDeclinedTransaction(testPan, DECLINED_AMOUNT, TEST_STANS[2]);
    }

    @AfterClass(alwaysRun = true)
    public void cleanUpDashboardTransactions() throws SQLException {
        deleteTestTransactions();
        deleteCurrentTestCard(testPan);
    }

    @Test(description = "TC-20 - Dashboard statistics and recent transactions")
    public void dashboardStatsAndRecentTransactionsMatchDatabase() throws Exception {
        SoftAssert soft = new SoftAssert();

        JsonNode stats = httpGet(GATEWAY_URL, "/api/dashboard/stats", 200);
        long totalTransactions = stats.path("totalTransactions").asLong();
        long approvedCount = stats.path("approvedCount").asLong();
        long declinedCount = stats.path("declinedCount").asLong();
        long totalAmount = stats.path("totalAmount").asLong();
        long averageAmount = stats.path("averageAmount").asLong();
        double approvalRate = stats.path("approvalRate").asDouble();
        double avgProcessingTimeMs = stats.path("avgProcessingTimeMs").asDouble();

        soft.assertTrue(totalTransactions > 0, "$.totalTransactions should be > 0");
        soft.assertEquals(totalTransactions, approvedCount + declinedCount,
                "$.totalTransactions should equal approvedCount + declinedCount");
        soft.assertEquals(approvalRate, (double) approvedCount / totalTransactions, 0.01,
                "$.approvalRate should match approvedCount / totalTransactions");
        soft.assertTrue(totalAmount > 0, "$.totalAmount should be > 0");
        soft.assertTrue(averageAmount > 0, "$.averageAmount should be > 0");
        soft.assertEquals(averageAmount, totalAmount / totalTransactions,
                "$.averageAmount should match totalAmount / totalTransactions");
        soft.assertTrue(avgProcessingTimeMs > 0, "$.avgProcessingTimeMs should be > 0");

        soft.assertEquals(dbUtils.queryLong("SELECT COUNT(*) FROM transactions"), totalTransactions,
                "DB total transactions count should match stats");
        soft.assertEquals(dbUtils.queryLong("SELECT COUNT(*) FROM transactions WHERE status = 'APPROVED'"),
                approvedCount, "DB APPROVED count should match stats");
        soft.assertEquals(dbUtils.queryLong("SELECT COUNT(*) FROM transactions WHERE status = 'DECLINED'"),
                declinedCount, "DB DECLINED count should match stats");
        soft.assertEquals(dbUtils.queryLong("SELECT COALESCE(SUM(amount), 0) FROM transactions"), totalAmount,
                "DB amount sum should match stats");
        soft.assertEquals(queryDouble("SELECT COALESCE(AVG(amount), 0) FROM transactions"), averageAmount, 1.0,
                "DB average amount should match stats");

        JsonNode recent = httpGet(GATEWAY_URL, "/api/dashboard/recent?limit=20", 200);
        soft.assertTrue(recent.isArray(), "Recent response should be an array");
        soft.assertTrue(recent.size() <= 20, "Recent response size should not exceed limit=20");
        soft.assertTrue(recent.size() > 0, "Recent response should not be empty");

        Instant previousCreatedAt = null;
        for (JsonNode transaction : recent) {
            soft.assertFalse(transaction.path("id").isMissingNode(), "Recent transaction should contain id");
            soft.assertFalse(transaction.path("pan").isMissingNode(), "Recent transaction should contain pan");
            soft.assertFalse(transaction.path("status").isMissingNode(), "Recent transaction should contain status");
            soft.assertFalse(transaction.path("amount").isMissingNode(), "Recent transaction should contain amount");
            soft.assertFalse(transaction.path("createdAt").isMissingNode(), "Recent transaction should contain createdAt");

            Instant currentCreatedAt = Instant.parse(transaction.path("createdAt").asText());
            if (previousCreatedAt != null) {
                soft.assertFalse(currentCreatedAt.isAfter(previousCreatedAt),
                        "Recent transactions should be sorted by createdAt DESC");
            }
            previousCreatedAt = currentCreatedAt;
        }

        String firstRecentId = recent.get(0).path("id").asText();
        String lastRecentId = recent.get(recent.size() - 1).path("id").asText();
        soft.assertEquals(firstRecentId, queryRecentTransactionId(0),
                "First recent transaction should match DB ordering");
        soft.assertEquals(lastRecentId, queryRecentTransactionId(recent.size() - 1),
                "Last recent transaction should match DB ordering");

        soft.assertAll();
    }

    private String createCard(String cardholderName, long initialBalance) {
        CreateCardRequest request = new CreateCardRequest(
                "400000",
                cardholderName,
                "643",
                1_000_000L,
                1_000_000L,
                initialBalance
        );

        return httpPost(GATEWAY_URL, "/api/cards", request, 201).path("pan").asText();
    }

    private void sendTransaction(String pan, long amount, String stan, String expectedStatus) throws Exception {
        AuthorizationRequest request = AuthorizationRequest.builder()
                .mti("0100")
                .stan(stan)
                .pan(pan)
                .processingCode("000000")
                .amount(amount)
                .currencyCode("643")
                .transmissionDateTime(LocalDateTime.now().toString())
                .terminalId("TERM0001")
                .terminalType("POS")
                .merchantId("MERCH0000000001")
                .mcc("5411")
                .acquirerId("ACQ001")
                .build();

        JsonNode response = httpPost(GATEWAY_URL, "/api/transactions", request, 200);
        assertEquals(response.path("status").asText(), expectedStatus);
        assertEquals(waitForTransactionStatus(stan), expectedStatus);
    }

    /*
    DECLINED-транзакция записывается напрямую в transaction-logger,
    потому что /api/transactions возвращает DECLINED клиенту,
    но не передаёт её в transaction-logger для сохранения
    */
    private void logDeclinedTransaction(String pan, long amount, String stan) throws Exception {
        Instant now = Instant.now();
        TransactionRequest request = new TransactionRequest(
                UUID.randomUUID(),
                "0100",
                stan,
                "999999" + stan,
                pan,
                "000000",
                amount,
                "643",
                "TERM0001",
                "POS",
                "MERCH0000000001",
                "5411",
                "ACQ001",
                "ISS001",
                0L,
                TransactionStatus.DECLINED,
                "INSUFFICIENT_FUNDS",
                null,
                1,
                now,
                now
        );

        httpPost(LOGGER_URL, "/api/internal/log", request, 201);
        assertEquals(waitForTransactionStatus(stan), TransactionStatus.DECLINED.name());
    }

    private String waitForTransactionStatus(String stan) throws Exception {
        Instant deadline = Instant.now().plusSeconds(5);
        while (Instant.now().isBefore(deadline)) {
            String status = queryOptionalString("SELECT status FROM transactions WHERE stan = ?", stan);
            if (status != null) {
                return status;
            }
            Thread.sleep(250);
        }
        throw new SQLException("No transaction found for stan: " + stan);
    }

    private void deleteTestTransactions() throws SQLException {
        executeUpdate("DELETE FROM transactions WHERE stan IN (?, ?, ?)", (Object[]) TEST_STANS);
    }

    private void deleteCurrentTestCard(String pan) throws SQLException {
        if (pan != null) {
            executeUpdate("DELETE FROM cards WHERE pan = ?", pan);
        }
    }

    private void deleteStaleTestCards() throws SQLException {
        executeUpdate("DELETE FROM cards WHERE cardholder_name = ?", TEST_CARDHOLDER_NAME);
    }

    private double queryDouble(String sql, Object... params) throws SQLException {
        try (Connection connection = dbUtils.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindParams(statement, params);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getDouble(1);
                }
            }
        }
        throw new IllegalStateException("Query returned no rows: " + sql);
    }

    private String queryOptionalString(String sql, Object... params) throws SQLException {
        try (Connection connection = dbUtils.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindParams(statement, params);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getString(1);
                }
            }
        }
        return null;
    }

    private String queryRecentTransactionId(int offset) throws SQLException {
        return dbUtils.queryString(
                "SELECT id::text FROM transactions ORDER BY created_at DESC LIMIT 1 OFFSET ?",
                offset
        );
    }

    private void executeUpdate(String sql, Object... params) throws SQLException {
        try (Connection connection = dbUtils.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindParams(statement, params);
            statement.executeUpdate();
        }
    }

    private static void bindParams(PreparedStatement statement, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            statement.setObject(i + 1, params[i]);
        }
    }
}

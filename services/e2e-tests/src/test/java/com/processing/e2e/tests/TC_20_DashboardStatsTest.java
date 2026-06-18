package com.processing.e2e.tests;

import com.fasterxml.jackson.databind.JsonNode;
import com.processing.common.dto.authorization.AuthorizationRequest;
import com.processing.common.dto.cardmanagement.CreateCardRequest;
import com.processing.e2e.E2EBaseTest;
import com.processing.e2e.utility.DBUtils;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.stream.IntStream;

import static org.testng.Assert.assertEquals;

public class TC_20_DashboardStatsTest extends E2EBaseTest {
    private static final String DASHBOARD_STATS_PATH = "/api/dashboard/stats";
    private static final String DASHBOARD_RECENT_PATH = "/api/dashboard/recent";
    private static final int RECENT_LIMIT = 20;
    private static final int TEST_TRANSACTION_COUNT = RECENT_LIMIT + 1;
    private static final String TEST_STAN_PREFIX = "20";
    private static final String[] TEST_STANS = IntStream.rangeClosed(1, TEST_TRANSACTION_COUNT)
            .mapToObj(index -> TEST_STAN_PREFIX + String.format("%04d", index))
            .toArray(String[]::new);
    private static final String TEST_CARDHOLDER_NAME = "DASHBOARD TEST USER";
    private static final int EXPECTED_APPROVED_COUNT = 15;
    private static final long EXPECTED_DECLINED_COUNT = TEST_TRANSACTION_COUNT - EXPECTED_APPROVED_COUNT;
    private static final BigDecimal START_AMOUNT = new BigDecimal("5000");
    private static final BigDecimal AMOUNT_STEP = new BigDecimal("5000");
    private static final BigDecimal INITIAL_BALANCE = IntStream.range(0, EXPECTED_APPROVED_COUNT)
            .mapToObj(TC_20_DashboardStatsTest::transactionAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    private static final BigDecimal EXPECTED_TOTAL_AMOUNT = IntStream.range(0, TEST_TRANSACTION_COUNT)
            .mapToObj(TC_20_DashboardStatsTest::transactionAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    private static final BigDecimal EXPECTED_AVERAGE_AMOUNT = EXPECTED_TOTAL_AMOUNT.divide(new BigDecimal(TEST_TRANSACTION_COUNT));
    private static final double EXPECTED_APPROVAL_RATE = (double) EXPECTED_APPROVED_COUNT / TEST_TRANSACTION_COUNT;

    private final DBUtils dbUtils = new DBUtils();
    private String testPan;

    @BeforeClass(alwaysRun = true)
    public void setUpDashboardTransactions() throws Exception {
        cleanUpDashboardTransactions();
        testPan = createCard(TEST_CARDHOLDER_NAME, INITIAL_BALANCE);

        for (int i = 0; i < TEST_STANS.length; i++) {
            String expectedStatus = i < EXPECTED_APPROVED_COUNT ? "APPROVED" : "DECLINED";
            sendTransaction(testPan, transactionAmount(i), TEST_STANS[i], expectedStatus);
            // Small pause so adjacent transactions get different createdAt values for stable recent-order checks.
            Thread.sleep(10);
        }
    }

    @AfterClass(alwaysRun = true)
    public void cleanUpDashboardTransactions() throws SQLException {
        deleteTransactions();
        deleteStaleTestCards();
    }

    @Test(description = "TC-20 - Dashboard statistics and recent transactions")
    public void dashboardStatsAndRecentTransactionsMatchDatabase() throws Exception {
        SoftAssert soft = new SoftAssert();

        JsonNode stats = httpGet(GATEWAY_URL, DASHBOARD_STATS_PATH, 200);
        long totalTransactions = stats.path("totalTransactions").asLong();
        long approvedCount = stats.path("approvedCount").asLong();
        long declinedCount = stats.path("declinedCount").asLong();
        BigDecimal totalAmount = stats.path("totalAmount").decimalValue();
        BigDecimal averageAmount = stats.path("averageAmount").decimalValue();
        double approvalRate = stats.path("approvalRate").asDouble();
        double avgProcessingTimeMs = stats.path("avgProcessingTimeMs").asDouble();

        soft.assertEquals(totalTransactions, TEST_TRANSACTION_COUNT,
                "$.totalTransactions should match prepared transaction count");
        soft.assertEquals(approvedCount, EXPECTED_APPROVED_COUNT,
                "$.approvedCount should match prepared approved transaction count");
        soft.assertEquals(declinedCount, EXPECTED_DECLINED_COUNT,
                "$.declinedCount should match prepared declined transaction count");
        soft.assertEquals(totalAmount.compareTo(EXPECTED_TOTAL_AMOUNT), 0,
                "$.totalAmount should match prepared transaction amount sum");
        soft.assertEquals(averageAmount.compareTo(EXPECTED_AVERAGE_AMOUNT), 0,
                "$.averageAmount should match prepared transaction average amount");
        soft.assertEquals(approvalRate, EXPECTED_APPROVAL_RATE, 0.01,
                "$.approvalRate should match prepared approved ratio");

        soft.assertTrue(totalTransactions > 0, "$.totalTransactions should be > 0");
        soft.assertEquals(totalTransactions, approvedCount + declinedCount,
                "$.totalTransactions should equal approvedCount + declinedCount");
        soft.assertEquals(approvalRate, (double) approvedCount / totalTransactions, 0.01,
                "$.approvalRate should match approvedCount / totalTransactions");
        soft.assertTrue(totalAmount.compareTo(BigDecimal.ZERO) > 0, "$.totalAmount should be > 0");
        soft.assertTrue(averageAmount.compareTo(BigDecimal.ZERO) > 0, "$.averageAmount should be > 0");
        soft.assertEquals(averageAmount.compareTo(totalAmount.divide(new BigDecimal(totalTransactions))), 0,
                "$.averageAmount should match totalAmount / totalTransactions");
        soft.assertTrue(avgProcessingTimeMs > 0, "$.avgProcessingTimeMs should be > 0");

        soft.assertEquals(dbUtils.queryLong("SELECT COUNT(*) FROM transactions"), totalTransactions,
                "DB total transactions count should match stats");
        soft.assertEquals(dbUtils.queryLong("SELECT COUNT(*) FROM transactions WHERE status = 'APPROVED'"),
                approvedCount, "DB APPROVED count should match stats");
        soft.assertEquals(dbUtils.queryLong("SELECT COUNT(*) FROM transactions WHERE status = 'DECLINED'"),
                declinedCount, "DB DECLINED count should match stats");
        soft.assertEquals(queryBigDecimal("SELECT COALESCE(SUM(amount), 0) FROM transactions").compareTo(totalAmount), 0,
                "DB amount sum should match stats");
        soft.assertEquals(queryBigDecimal("SELECT COALESCE(AVG(amount), 0) FROM transactions").compareTo(averageAmount), 0,
                "DB average amount should match stats");

        JsonNode recent = httpGet(GATEWAY_URL, DASHBOARD_RECENT_PATH + "?limit=" + RECENT_LIMIT, 200);
        soft.assertTrue(recent.isArray(), "Recent response should be an array");
        soft.assertEquals(recent.size(), RECENT_LIMIT,
                "Recent response size should match limit when more transactions exist");

        String previousId = null;
        Instant previousCreatedAt = null;
        for (int i = 0; i < recent.size(); i++) {
            JsonNode transaction = recent.get(i);
            soft.assertFalse(transaction.path("id").isMissingNode(), "Recent transaction should contain id");
            soft.assertFalse(transaction.path("pan").isMissingNode(), "Recent transaction should contain pan");
            soft.assertFalse(transaction.path("status").isMissingNode(), "Recent transaction should contain status");
            soft.assertFalse(transaction.path("amount").isMissingNode(), "Recent transaction should contain amount");
            soft.assertFalse(transaction.path("createdAt").isMissingNode(), "Recent transaction should contain createdAt");

            String currentId = transaction.path("id").asText();
            Instant currentCreatedAt = Instant.parse(transaction.path("createdAt").asText());
            if (previousCreatedAt != null) {
                soft.assertFalse(currentCreatedAt.isAfter(previousCreatedAt),
                        "Recent transaction at index " + i
                                + " should not be newer than previous transaction. "
                                + "previousId=" + previousId
                                + ", previousCreatedAt=" + previousCreatedAt
                                + ", currentId=" + currentId
                                + ", currentCreatedAt=" + currentCreatedAt);
            }
            previousId = currentId;
            previousCreatedAt = currentCreatedAt;
        }

        for (int i = 0; i < recent.size(); i++) {
            soft.assertEquals(recent.get(i).path("id").asText(), queryRecentTransactionId(i),
                    "Recent transaction at index " + i + " should match DB ordering");
        }

        soft.assertAll();
    }

    private static BigDecimal transactionAmount(int index) {
        return START_AMOUNT.add(AMOUNT_STEP.multiply(new BigDecimal(index)));
    }

    private String createCard(String cardholderName, BigDecimal initialBalance) {
        CreateCardRequest request = new CreateCardRequest(
                "400000",
                cardholderName,
                "643",
                new BigDecimal("1000000"),
                new BigDecimal("1000000"),
                initialBalance
        );

        return httpPost(GATEWAY_URL, "/api/cards", request, 201).path("pan").asText();
    }

    private void sendTransaction(String pan, BigDecimal amount, String stan, String expectedStatus) throws Exception {
        AuthorizationRequest request = AuthorizationRequest.builder()
                .mti("0100")
                .stan(stan)
                .pan(pan)
                .processingCode("000000")
                .amount(amount)
                .currencyCode("643")
                .transmissionDateTime(Instant.now())
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

    private void deleteTransactions() throws SQLException {
        executeUpdate("DELETE FROM transactions");
    }

    private void deleteStaleTestCards() throws SQLException {
        executeUpdate("DELETE FROM cards WHERE cardholder_name = ?", TEST_CARDHOLDER_NAME);
    }

    private BigDecimal queryBigDecimal(String sql, Object... params) throws SQLException {
        try (Connection connection = dbUtils.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindParams(statement, params);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    BigDecimal value = resultSet.getBigDecimal(1);
                    return value != null ? value : BigDecimal.ZERO;
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

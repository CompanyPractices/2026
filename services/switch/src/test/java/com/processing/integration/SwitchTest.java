package com.processing.integration;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.sql.*;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.testng.Assert.*;


public class SwitchTest {

    private static final String GATEWAY_URL = "http://localhost:8080";
    private static final String DB_URL = "jdbc:postgresql://localhost:5432/smp_db";
    private static final String DB_USER = "smp_user";
    private static final String DB_PASSWORD = "smp_password";

    private Connection connection;

    @BeforeClass
    public void setUp() throws SQLException {
        RestAssured.baseURI = GATEWAY_URL;
        connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        ensureTestCardsExist();
    }

    @AfterClass
    public void tearDown() throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(
                "DELETE FROM cards WHERE cardholder_name = 'INTEGRATION TEST'")) {
            stmt.executeUpdate();
        }
        if (connection != null) {
            connection.close();
        }
    }

    private void ensureTestCardsExist() {
        String[] bins = {"400000", "400001", "400002", "400003", "400004"};
        for (String bin : bins) {
            given()
                    .contentType(ContentType.JSON)
                    .body(String.format(
                            "{\"bin\":\"%s\",\"cardholderName\":\"INTEGRATION TEST\"," +
                                    "\"currencyCode\":\"643\",\"dailyLimit\":15000000," +
                                    "\"monthlyLimit\":300000000,\"initialBalance\":100000000}", bin))
                    .when()
                    .post("/api/cards")
                    .then()
                    .statusCode(201);
        }
    }

    @Test(description = "TC-14: Маршрутизация по BIN (5 BIN → 5 issuerId)")
    public void shouldRouteTransactionToCorrectIssuerBasedOnBin() throws SQLException {
        String[] bins = {"400000", "400001", "400002", "400003", "400004"};
        String[] expectedIssuers = {"ISS001", "ISS002", "ISS003", "ISS004", "ISS005"};

        for (int i = 0; i < bins.length; i++) {
            String pan = getActiveCardByBin(bins[i]);
            String stan = generateStan();

            given()
                    .contentType(ContentType.JSON)
                    .body(transactionPayload(stan, pan, 1000))
                    .when()
                    .post("/api/transactions")
                    .then()
                    .statusCode(200);

            String issuerId = getIssuerIdFromDb(stan);
            assertNotNull(issuerId, "Transaction " + stan + " not found in DB");
            assertEquals(issuerId, expectedIssuers[i],
                    "BIN " + bins[i] + " → " + expectedIssuers[i] + ", actual: " + issuerId);
        }
    }


    @Test(description = "TC-06: Полный цикл одиночной транзакции (APPROVED)")
    public void shouldCompleteFullTransactionCycleAndDecreaseBalance() throws SQLException {
        String pan = getActiveCardWithBalance(1000);
        long balanceBefore = getCardBalance(pan);
        String stan = generateStan();

        given()
                .contentType(ContentType.JSON)
                .body(transactionPayload(stan, pan, 1000))
                .when()
                .post("/api/transactions")
                .then()
                .statusCode(200)
                .body("status", equalTo("APPROVED"))
                .body("responseCode", equalTo("00"))
                .body("rrn", matchesPattern("\\d{12}"))
                .body("authCode", matchesPattern("[A-Z0-9]{6}"));

        assertEquals(getCardBalance(pan), balanceBefore - 1000,
                "Balance should decrease by 1000");

        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT status, issuer_id FROM transactions WHERE stan = ?")) {
            stmt.setString(1, stan);
            ResultSet rs = stmt.executeQuery();
            assertTrue(rs.next(), "Transaction " + stan + " not found in DB");
            assertEquals(rs.getString("status"), "APPROVED");
            assertNotNull(rs.getString("issuer_id"), "issuer_id must not be null");
        }
    }


    private String transactionPayload(String stan, String pan, int amount) {
        return String.format(
                "{\"mti\":\"0100\",\"stan\":\"%s\",\"pan\":\"%s\"," +
                        "\"processingCode\":\"000000\",\"amount\":%d,\"currencyCode\":\"643\"," +
                        "\"transmissionDateTime\":\"2026-06-01T10:30:00Z\"," +
                        "\"terminalId\":\"TERM001\",\"merchantId\":\"MERCH00000000001\"," +
                        "\"mcc\":\"5411\",\"acquirerId\":\"ACQ001\"}",
                stan, pan, amount);
    }

    private String getIssuerIdFromDb(String stan) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT issuer_id FROM transactions WHERE stan = ?")) {
            stmt.setString(1, stan);
            ResultSet rs = stmt.executeQuery();
            return rs.next() ? rs.getString("issuer_id") : null;
        }
    }

    private String getActiveCardWithBalance(long minBalance) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT pan FROM cards WHERE status = 'ACTIVE' AND available_balance >= ? LIMIT 1")) {
            stmt.setLong(1, minBalance);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getString("pan");
        }
        throw new IllegalStateException("No active card with balance >= " + minBalance);
    }

    private String getActiveCardByBin(String bin) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT pan FROM cards WHERE bin = ? AND status = 'ACTIVE' LIMIT 1")) {
            stmt.setString(1, bin);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getString("pan");
        }
        throw new IllegalStateException("No active card for BIN " + bin);
    }

    private long getCardBalance(String pan) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT available_balance FROM cards WHERE pan = ?")) {
            stmt.setString(1, pan);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getLong("available_balance");
        }
        throw new IllegalStateException("Card not found: " + pan);
    }

    private String generateStan() {
        return String.format("%06d", (int) (Math.random() * 999999));
    }
}
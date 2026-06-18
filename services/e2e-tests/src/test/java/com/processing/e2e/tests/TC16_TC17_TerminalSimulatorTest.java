package com.processing.e2e.tests;

import com.fasterxml.jackson.databind.JsonNode;
import com.processing.e2e.E2EBaseTest;
import com.processing.e2e.utility.DBUtils;
import lombok.extern.slf4j.Slf4j;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.testng.Assert.*;

/**
 * E2E тесты для terminal-simulator.
 * Проверяют, что симулятор генерирует транзакции, шлюз их обрабатывает,
 * и состояние карт/транзакций в БД корректно.
 */
@Slf4j
public class TC16_TC17_TerminalSimulatorTest extends E2EBaseTest {

    private static final String SIMULATOR_RUN_URL = "/api/simulator/terminal/run";
    private static final DBUtils dbUtils = new DBUtils();

    @BeforeClass
    public void setup() {
        RestAssured.baseURI = GATEWAY_URL;
        ensureActiveCardsExist();
    }

    @Test(description = "TC-16: Сценарий 'normal'")
    public void testNormalScenario() throws Exception {
        long countBefore = dbUtils.queryLong("SELECT COUNT(*) from transactions");
        String testStart = dbUtils.queryString("select now()");

        JsonNode runResponse = given()
                .baseUri(TERMINAL_SIM_URL)
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "count": 50,
                            "scenario": "normal"
                        }
                        """)
                .when()
                .post(SIMULATOR_RUN_URL)
                .then()
                .statusCode(200)
                .body("totalSubmitted", equalTo(50))
                .body("approved", greaterThanOrEqualTo(0))
                .body("declined", greaterThanOrEqualTo(0))
                .extract()
                .as(JsonNode.class);

        int httpApproved = runResponse.get("approved").asInt();
        int httpDeclined = runResponse.get("declined").asInt();
        assertEquals(httpApproved + httpDeclined, 50,
                "http response transactions count != http request transactions count");

        JsonNode transactions = runResponse.get("transactions");
        assertEquals(transactions.size(), 50);
        for (JsonNode transaction : transactions) {
            if (!"APPROVED".equals(transaction.path("status").asText())) {
                log.info("Terminal-simulator e2e: declined transaction in HTTP response: {}",
                        transaction.toPrettyString());
            }
            assertNotNull(transaction.get("status"), "transaction status in response is null");
            assertNotNull(transaction.get("stan"), "transaction stan in response is null");
            assertNotNull(transaction.get("mti"),  "transaction mti in response is null");
        }

        long dbCount = dbUtils.queryLong("SELECT COUNT(*) FROM transactions");
        long dbApprovedCount = dbUtils.queryLong(
                "SELECT count(*) FROM transactions WHERE created_at > ?::timestamp AND status='APPROVED'",
                testStart);
        long dbDeclinedCount = dbUtils.queryLong(
                "SELECT count(*) FROM transactions WHERE created_at > ?::timestamp AND status='DECLINED'",
                testStart);
        dbCount = dbCount - countBefore;

        assertEquals(dbCount, 50, "50 transactions should appear in DB");
        assertEquals(dbApprovedCount + dbDeclinedCount, 50,
                "DB transactions count != response transactions count");
        log.info("Terminal-simulator e2e: http approved count: {}, db approved count: {}", httpApproved,
                dbApprovedCount);
        log.info("Terminal-simulator e2e: http declined count: {}, db declined count: {}", httpDeclined,
                dbDeclinedCount);
        assertEquals(dbApprovedCount, httpApproved,
                "http approved transactions count != db approved transactions count");
        assertEquals(dbDeclinedCount, httpDeclined,
                "http declined transactions count != db declined transactions count");

        String mcc = dbUtils.queryString("SELECT DISTINCT mcc FROM transactions WHERE created_at > ?::timestamp",
                testStart);
        assertEquals(mcc, "5411", "mcc should be only 5411");
    }

    @Test(description = "TC-17: Сценарий 'declines_test'")
    public void testDeclines() throws Exception {
        DBUtils dbUtils = new DBUtils();
        long countBefore = dbUtils.queryLong("SELECT COUNT(*) from transactions");

        JsonNode runResponse = given()
                .baseUri(TERMINAL_SIM_URL)
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "count": 50,
                            "scenario": "declines_test"
                        }
                        """)
                .when()
                .post(SIMULATOR_RUN_URL)
                .then()
                .statusCode(200)
                .body("totalSubmitted", equalTo(50))
                .body("declined", greaterThanOrEqualTo(0))
                .extract()
                .as(JsonNode.class);

        int httpApproved = runResponse.get("approved").asInt();
        int httpDeclined = runResponse.get("declined").asInt();
        int cardNotFound = 0;
        int blocked = 0;
        int insufficientFunds = 0;
        int exceedsAmountLimit = 0;

        JsonNode transactions = runResponse.get("transactions");
        assertEquals(transactions.size(), 50);
        for (JsonNode transaction : transactions) {
            switch(transaction.get("responseCode").asText()) {
                case "14" -> cardNotFound++;             // CODE_CARD_NOT_FOUND
                case "05" -> blocked++;                  // CODE_DECLINED_GENERAL?
                case "51" -> insufficientFunds++;        // CODE_INSUFFICIENT_FUNDS
                case "61" -> exceedsAmountLimit++;                  // EXCEEDS_AMOUNT_LIMIT
            }
        }
        assertNotEquals(cardNotFound, 0,
                "No cardNotFound code responses found in declines_test scenario");
        assertNotEquals(blocked, 0, "No blocked code responses found in declines_test scenario");
        assertNotEquals(insufficientFunds, 0,
                "No insufficientFunds code responses found in declines_test scenario");
        assertNotEquals(exceedsAmountLimit, 0,
                "No exceedsAmountLimit code responses found in declines_test scenario");


        long dbCount = dbUtils.queryLong("SELECT COUNT(*) FROM transactions");
        dbCount = dbCount - countBefore;

        assertEquals(dbCount, 50, "50 transactions should appear in DB");
        assertEquals(httpApproved + httpDeclined, dbCount,
                "DB transactions count != http response transactions count");
    }

    private void ensureActiveCardsExist() {
        JsonNode health = httpGet(CARD_MGMT_URL, "/health", 200);
        if (!"ok".equals(health.path("status").asText())) {
            throw new IllegalStateException("Card-management not healthy");
        }

        given()
                .contentType(ContentType.JSON)
                .body("""
                {
                                "count": 100,
                                "bins": ["400000","400001","400002","400003","400004"]
                                }""")
                .when()
                .post("/api/cards/generate")
                .then()
                .statusCode(201);
    }
}

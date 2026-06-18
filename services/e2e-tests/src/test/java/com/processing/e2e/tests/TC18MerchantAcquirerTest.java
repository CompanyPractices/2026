package com.processing.e2e.tests;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.processing.e2e.E2EBaseTest;
import com.processing.e2e.utility.DBUtils;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.sql.SQLException;

import static io.restassured.RestAssured.given;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;


public class TC18MerchantAcquirerTest extends E2EBaseTest {
    private static final int TX_COUNT = 100;
    private final DBUtils dbUtils = new DBUtils();

    @BeforeClass
    public void setUp() {
        RestAssured.baseURI = GATEWAY_URL;
    }

    @Test
    public void tc_18_merchantAcquirerGroceryScenario() throws SQLException, JsonProcessingException {
        given()
                .contentType(ContentType.JSON)
                .body("""
                {
                                "count": 100,
                                "bins": ["400000"]
                                }""")
                .when()
                .post("/api/cards/generate")
                .then()
                .statusCode(201);

        long countBefore = dbUtils.queryLong("select count(*) from transactions");
        long countFeesBefore = dbUtils.queryLong("select count(*) from acquirer_fee where acquirer_fee > 0");
        String testStart = dbUtils.queryString("select now()");

        Response response = given()
                .contentType(ContentType.JSON)
                .body("""
                {
                                "count": 100,
                                "scenario": "grocery",
                                "mccCodes": ["5411"]
                                }""")
                .when()
                .post("/api/simulator/merchant/run")
                .then()
                .statusCode(200)
                .extract().response();

        JsonNode body = mapper.readTree(response.asString());
        assertEquals(TX_COUNT, body.get("totalSubmitted").asInt());

        JsonNode transactions = body.get("transactions");
        assertNotNull(transactions);
        assertEquals(TX_COUNT, transactions.size());

        for(JsonNode node: transactions){
            assertNotNull(node.get("status"));
            assertNotNull(node.get("stan"));
        }

        long countAfter = dbUtils.queryLong("select count(*) from transactions");
        long transactionsCount = dbUtils.queryLong("select count(*) from transactions where created_at > ?::timestamp", testStart);

        assertEquals(TX_COUNT, transactionsCount);
        assertEquals(countBefore, countAfter - TX_COUNT);

        long groceryCount = dbUtils.queryLong("select count(*) from transactions where mcc = '5411' and created_at > ?::timestamp", testStart);
        assertEquals(groceryCount, transactionsCount);

        long feeRows = dbUtils.queryLong("select count(*) from acquirer_fee where acquirer_fee > 0");
        assertEquals(TX_COUNT, feeRows - countFeesBefore);
    }
}

package com.processing.e2e.tests;

import com.processing.common.dto.cardmanagement.CardModel;
import com.processing.common.dto.cardmanagement.ReserveRequest;
import com.processing.e2e.E2EBaseTest;
import com.processing.e2e.utility.DBUtils;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.math.BigDecimal;

import static org.testng.Assert.assertEquals;

public class TC_15_ReserveFunds extends E2EBaseTest {
    private final DBUtils dbUtils = new DBUtils();

    private static final BigDecimal FIRST_RESERVE = new BigDecimal(500000);
    private static final BigDecimal SECOND_RESERVE = new BigDecimal(300000);
    private static final String FIRST_RRN = "123456789012";
    private static final String SECOND_RRN = "123456789013";

    @AfterMethod()
    public void tearDown() {
        try {
            dbUtils.executeUpdate("DELETE FROM reservations WHERE rrn IN (?, ?)", FIRST_RRN, SECOND_RRN);
        } catch (Exception ignored) {
        }
    }

    @Test(description = "TC-15 -  Резервирование средств (POST /api/cards/{pan}/reserve)")
    public void tc15_reserveFunds() throws Exception {

        CardModel createdCard = mapper.treeToValue(
                httpPost(GATEWAY_URL, "/api/cards", createCardRequest, 201),
                CardModel.class
        );

        BigDecimal balanceBefore = new BigDecimal(dbUtils.queryLong("SELECT available_balance FROM cards WHERE pan = ?", createdCard.pan()));
        assertEquals(balanceBefore.compareTo(createdCard.availableBalance()), 0);

        httpUtils.httpPost(GATEWAY_URL, "/api/cards/" + createdCard.pan() + "/reserve",
                new ReserveRequest(FIRST_RESERVE, FIRST_RRN), 200);
        BigDecimal expectedAfterFirst = balanceBefore.subtract(FIRST_RESERVE);

        BigDecimal balanceAfterFirstReserve = new BigDecimal(
                dbUtils.queryLong("SELECT available_balance FROM cards WHERE pan = ?", createdCard.pan()));
        assertEquals(balanceAfterFirstReserve.compareTo(expectedAfterFirst), 0);

        CardModel cardAfterFirstReserve = mapper.treeToValue(
                httpUtils.httpGet(GATEWAY_URL, "/api/cards/" + createdCard.pan(), 200),
                CardModel.class
        );
        assertEquals(cardAfterFirstReserve.availableBalance().compareTo(expectedAfterFirst), 0);

        httpUtils.httpPost(GATEWAY_URL, "/api/cards/" + createdCard.pan() + "/reserve",
                new ReserveRequest(SECOND_RESERVE, SECOND_RRN), 200);
        BigDecimal expectedAfterSecond = balanceBefore.subtract(FIRST_RESERVE).subtract(SECOND_RESERVE);

        BigDecimal balanceAfterSecondReserve = new BigDecimal(
                dbUtils.queryLong("SELECT available_balance FROM cards WHERE pan = ?", createdCard.pan()));
        assertEquals(balanceAfterSecondReserve.compareTo(expectedAfterSecond), 0);
    }
}

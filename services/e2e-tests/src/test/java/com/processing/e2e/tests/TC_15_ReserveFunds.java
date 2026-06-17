package com.processing.e2e.tests;

import com.processing.common.dto.cardmanagement.CardModel;
import com.processing.common.dto.cardmanagement.ReserveRequest;
import com.processing.e2e.E2EBaseTest;
import com.processing.e2e.utility.DBUtils;
import org.testng.annotations.Test;

import java.math.BigDecimal;

import static org.testng.Assert.assertEquals;

public class TC_15_ReserveFunds extends E2EBaseTest {
    private final DBUtils dbUtils = new DBUtils();

    @Test(description = "TC-15 -  Резервирование средств (POST /api/cards/{pan}/reserve)")
    public void tc15_reserveFunds() throws Exception {
        BigDecimal firstReserve = new BigDecimal(500000);
        BigDecimal secondReserve = new BigDecimal(300000);
        String firstRrn = "123456789012";
        String secondRrn = "123456789013";

        CardModel createdCard = mapper.treeToValue(
                httpPost(GATEWAY_URL, "/api/cards", createCardRequest, 201),
                CardModel.class
        );

        BigDecimal balanceBefore = new BigDecimal(dbUtils.queryLong("SELECT available_balance FROM cards WHERE pan = ?", createdCard.pan()));
        assertEquals(balanceBefore.compareTo(createdCard.availableBalance()), 0);

        httpUtils.httpPost(GATEWAY_URL, "/api/cards/" + createdCard.pan() + "/reserve",
                new ReserveRequest(firstReserve, firstRrn), 200);
        BigDecimal expectedAfterFirst = balanceBefore.subtract(firstReserve);

        BigDecimal balanceAfterFirstReserve = new BigDecimal(
                dbUtils.queryLong("SELECT available_balance FROM cards WHERE pan = ?", createdCard.pan()));
        assertEquals(balanceAfterFirstReserve.compareTo(expectedAfterFirst), 0);

        CardModel cardAfterFirstReserve = mapper.treeToValue(
                httpUtils.httpGet(GATEWAY_URL, "/api/cards/" + createdCard.pan(), 200),
                CardModel.class
        );
        assertEquals(cardAfterFirstReserve.availableBalance().compareTo(expectedAfterFirst), 0);

        httpUtils.httpPost(GATEWAY_URL, "/api/cards/" + createdCard.pan() + "/reserve",
                new ReserveRequest(secondReserve, secondRrn), 200);
        BigDecimal expectedAfterSecond = balanceBefore.subtract(firstReserve).subtract(secondReserve);

        BigDecimal balanceAfterSecondReserve = new BigDecimal(
                dbUtils.queryLong("SELECT available_balance FROM cards WHERE pan = ?", createdCard.pan()));
        assertEquals(balanceAfterSecondReserve.compareTo(expectedAfterSecond), 0);
    }
}

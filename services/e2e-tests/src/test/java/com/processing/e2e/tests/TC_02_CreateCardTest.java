package com.processing.e2e.tests;

import com.processing.common.dto.cardmanagement.CardModel;
import com.processing.common.dto.cardmanagement.CardModelStatus;
import com.processing.e2e.E2EBaseTest;
import com.processing.e2e.utility.DBUtils;
import org.testng.annotations.Test;

import java.util.Map;

import static org.testng.Assert.assertEquals;

public class TC_02_CreateCardTest extends E2EBaseTest {
    private final DBUtils dbUtils = new DBUtils();

    @Test(description = "TC_02 - создание карты (POST /api/cards)")
    public void tc02_createCard() throws Exception {
        CardModel createdCard = mapper.treeToValue(
                httpPost(GATEWAY_URL, "/api/cards", createCardRequest, 201),
                CardModel.class
        );

        CardModel expectedCard = new CardModel(
                createdCard.id(),
                createdCard.pan(),
                createCardRequest.bin(),
                createCardRequest.cardholderName(),
                createdCard.expiryDate(),
                CardModelStatus.ACTIVE,
                createCardRequest.currencyCode(),
                createCardRequest.dailyLimit(),
                createCardRequest.monthlyLimit(),
                createCardRequest.initialBalance(),
                createdCard.issuerId(),
                createdCard.createdAt()
        );

        assertEquals(createdCard, expectedCard);

        Map<String, Object> row = dbUtils.queryRow(
                "SELECT * FROM cards WHERE pan = ?", createdCard.pan()
        );

        CardModel dbCard = cardFromRow(row);

        CardModel expectedDbCard = new CardModel(
                createdCard.id(),
                createdCard.pan(),
                createCardRequest.bin(),
                createCardRequest.cardholderName(),
                createdCard.expiryDate(),
                CardModelStatus.ACTIVE,
                createCardRequest.currencyCode(),
                createCardRequest.dailyLimit(),
                createCardRequest.monthlyLimit(),
                createCardRequest.initialBalance(),
                dbCard.issuerId(),
                dbCard.createdAt()
        );

        assertEquals(dbCard, expectedDbCard);
    }
}

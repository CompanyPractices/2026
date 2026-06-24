package com.processing.e2e.tests;

import com.processing.common.dto.cardmanagement.CardModel;
import com.processing.e2e.E2EBaseTest;
import com.processing.e2e.utility.DBUtils;
import org.testng.annotations.Test;

import java.util.Map;

import static org.testng.Assert.assertEquals;

public class TC_04_GetCardByPan extends E2EBaseTest {

    private final DBUtils dbUtils = new DBUtils();

    @Test(description = "TC-04 - получение карты по PAN и 404 для несуществующей")
    public void tc04_getCardByPan() throws Exception {
        CardModel createdCard = mapper.treeToValue(
                httpPost(GATEWAY_URL, "/api/cards", createCardRequest, 201),
                CardModel.class
        );

        CardModel card = mapper.treeToValue(
                httpUtils.httpGet(GATEWAY_URL, "/api/cards/" + createdCard.pan(), 200),
                CardModel.class
        );

        CardModel expectedCard = new CardModel(
                createdCard.id(),
                createdCard.pan(),
                createdCard.bin(),
                createdCard.cardholderName(),
                createdCard.expiryDate(),
                createdCard.status(),
                createdCard.currencyCode(),
                createdCard.dailyLimit(),
                createdCard.monthlyLimit(),
                createdCard.availableBalance(),
                createdCard.issuerId(),
                card.createdAt()
        );

        Map<String, Object> row = dbUtils.queryRow(
                "SELECT * FROM cards WHERE pan = ?", card.pan()
        );
        CardModel dbCard = cardFromRow(row);

        assertEquals(dbCard, expectedCard);

        httpUtils.httpGet(GATEWAY_URL, "/api/cards/0000000000000000", 404);
        long count = dbUtils.queryLong("SELECT COUNT(*) FROM cards WHERE pan = '0000000000000000'");
        assertEquals(count, 0);
    }
}

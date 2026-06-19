package com.processing.e2e.tests;

import com.processing.common.dto.cardmanagement.CardModel;
import com.processing.common.dto.cardmanagement.CardModelStatus;
import com.processing.common.dto.cardmanagement.PatchCardRequest;
import com.processing.e2e.E2EBaseTest;
import com.processing.e2e.utility.DBUtils;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class TC_05_PatchAndDeleteCard extends E2EBaseTest {
    private final DBUtils dbUtils = new DBUtils();

    @Test(description = "TC-05 - PATCH и DELETE карты")
    public void tc05_patchAndDeleteCard() throws Exception {
        CardModel createdCard = mapper.treeToValue(
                httpPost(GATEWAY_URL, "/api/cards", createCardRequest, 201),
                CardModel.class
        );

        CardModel dbCard = cardFromRow(dbUtils.queryRow("SELECT * FROM cards WHERE pan = ?", createdCard.pan()));
        assertEquals(dbCard.status(), CardModelStatus.ACTIVE);

        httpUtils.httpPatch(
                GATEWAY_URL,
                "/api/cards/" + createdCard.pan(),
                new PatchCardRequest(CardModelStatus.BLOCKED, null, null, null),
                200);

        CardModel updatedDbCard = cardFromRow(dbUtils.queryRow("SELECT * FROM cards WHERE pan = ?", createdCard.pan()));
        assertEquals(updatedDbCard.status(), CardModelStatus.BLOCKED);

        CardModel updatedCard = mapper.treeToValue(
                httpUtils.httpGet(GATEWAY_URL, "/api/cards/" + createdCard.pan(), 200),
                CardModel.class
        );

        assertEquals(updatedCard.status(), CardModelStatus.BLOCKED);
        assertEquals(updatedCard.status(), updatedDbCard.status());

        httpUtils.httpDelete(GATEWAY_URL, "/api/cards/" + createdCard.pan(), 204);

        String dbStatus = dbUtils.queryString("SELECT status FROM cards WHERE pan = ?", createdCard.pan());
        assertEquals(dbStatus, "DELETED");

        httpUtils.httpGet(GATEWAY_URL, "/api/cards/" + createdCard.pan(), 404);
    }
}

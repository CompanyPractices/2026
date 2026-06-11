package com.processing.e2e.tests;

import com.processing.common.dto.cardmanagement.CardModel;
import com.processing.common.dto.cardmanagement.CardModelStatus;
import com.processing.common.dto.cardmanagement.CreateCardRequest;
import com.processing.e2e.E2EBaseTest;
import com.processing.e2e.utility.DBUtils;
import io.restassured.RestAssured;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.sql.Timestamp;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

import static org.testng.Assert.assertEquals;

public class CardManagementTest extends E2EBaseTest {

    private final DBUtils dbUtils = new DBUtils();

    @BeforeClass
    public void setUp() {
        RestAssured.baseURI = GATEWAY_URL;
    }

    @Test(description = "TC_02 - создание карты")
    public void createCardShouldReturn201AndSaveToDb() throws Exception {
        CreateCardRequest request = new CreateCardRequest(
                "400000",
                "IVAN PETROV",
                "643",
                15000000L,
                300000000L,
                100000000L
        );

        CardModel card = mapper.treeToValue(
                httpUtils.httpPost(GATEWAY_URL, "/api/cards", request, 201),
                CardModel.class
        );

        CardModel expectedCard = new CardModel(
                card.id(),
                card.pan(),
                request.bin(),
                request.cardholderName(),
                card.expiryDate(),
                CardModelStatus.ACTIVE,
                request.currencyCode(),
                request.dailyLimit(),
                request.monthlyLimit(),
                request.initialBalance(),
                card.issuerId(),
                card.createdAt()
        );

        assertEquals(card, expectedCard);

        Map<String, Object> row = dbUtils.queryRow(
                "SELECT * FROM cards WHERE pan = ?", card.pan()
        );

        CardModel dbCard = cardFromRow(row);

        CardModel expectedDbCard = new CardModel(
                card.id(),
                card.pan(),
                request.bin(),
                request.cardholderName(),
                dbCard.expiryDate(),
                CardModelStatus.ACTIVE,
                request.currencyCode(),
                request.dailyLimit(),
                request.monthlyLimit(),
                request.initialBalance(),
                dbCard.issuerId(),
                dbCard.createdAt()
        );

        assertEquals(dbCard, expectedDbCard);
    }

    private String parseExpiryDate(Object dbValue) {
        if (dbValue == null) {
            return null;
        }

        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("MMyy");
        YearMonth yearMonth = YearMonth.parse(dbValue.toString(), inputFormatter);

        return yearMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"));
    }

    private CardModel cardFromRow(Map<String, Object> row) {
        return new CardModel(
                (UUID) row.get("id"),
                (String) row.get("pan"),
                (String) row.get("bin"),
                (String) row.get("cardholder_name"),
                YearMonth.parse(row.get("expiry_date").toString(),
                        DateTimeFormatter.ofPattern("MMyy")),
                CardModelStatus.valueOf((String) row.get("status")),
                (String) row.get("currency_code"),
                ((Number) row.get("daily_limit")).longValue(),
                ((Number) row.get("monthly_limit")).longValue(),
                ((Number) row.get("available_balance")).longValue(),
                (String) row.get("issuer_id"),
                ((Timestamp) row.get("created_at")).toLocalDateTime()
        );
    }
}

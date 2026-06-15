package com.processing.e2e.tests;

import com.processing.common.dto.cardmanagement.*;
import com.processing.e2e.E2EBaseTest;
import com.processing.e2e.utility.DBUtils;
import io.restassured.RestAssured;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.sql.Timestamp;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.testng.Assert.*;

public class CardManagementTest extends E2EBaseTest {

    private final DBUtils dbUtils = new DBUtils();

    private static final List<String> BINS = List.of("400000", "400001", "400002", "400003", "400004");

    private static final CreateCardRequest createCardRequest = new CreateCardRequest(
            "400000",
            "IVAN PETROV",
            "643",
            15000000L,
            300000000L,
            100000000L
    );

    @BeforeMethod
    public void setUp() throws Exception {
        RestAssured.baseURI = GATEWAY_URL;
        dbUtils.execute("DELETE FROM cards");
    }

    private CardModel createTestCard() throws Exception {
        return mapper.treeToValue(
                httpUtils.httpPost(GATEWAY_URL, "/api/cards", createCardRequest, 201),
                CardModel.class
        );
    }

    @Test(description = "TC_02 - создание карты (POST /api/cards)")
    public void tc02_createCard() throws Exception {
        CardModel createdCard = createTestCard();

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

    @Test(description = "TC-03 - массовая генерация тестовых карт")
    public void tc03_generateCards() throws Exception {
        GenerateCardsRequest request = new GenerateCardsRequest(100, BINS);
        GenerateCardResponse response = mapper.treeToValue(
                httpUtils.httpPost(GATEWAY_URL, "/api/cards/generate", request, 201),
                GenerateCardResponse.class
        );
        long distinctPansCount = response.cards().stream().distinct().count();

        assertEquals(response.generated(), 100, "generated must be 100");
        assertEquals(response.cards().size(), response.generated(), "cards array length must match generated");
        assertEquals(distinctPansCount, response.generated(), "pans must be unique");

        response.cards().forEach(card -> {
            assertNotNull(card.id(), "id must not be null");
            assertNotNull(card.status(), "status must not be null");
        });

        Set<String> binsPresent = response.cards().stream()
                .map(CardModel::bin)
                .collect(Collectors.toSet());

        BINS.forEach(bin -> assertTrue(binsPresent.contains(bin), "missing bin in response: " + bin));

        long dbCount = dbUtils.queryLong("SELECT COUNT(*) FROM cards WHERE status <> 'DELETED'");

        assertEquals(dbCount, response.generated(), "db must save all cards");

        List<String> dbBins = dbUtils.queryStringList("SELECT DISTINCT bin FROM cards");
        BINS.forEach(bin -> assertTrue(dbBins.contains(bin), "missing bin in db: " + bin));
    }

    @Test(description = "TC-04 - получение карты по PAN и 404 для несуществующей")
    public void tc04_getCardByPan() throws Exception {
        CardModel createdCard = createTestCard();

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

        assertEquals(card, expectedCard);

        Map<String, Object> row = dbUtils.queryRow(
                "SELECT * FROM cards WHERE pan = ?", card.pan()
        );
        CardModel dbCard = cardFromRow(row);

        assertEquals(dbCard, expectedCard);

        httpUtils.httpGet(GATEWAY_URL, "/api/cards/0000000000000000", 404);
        long count = dbUtils.queryLong("SELECT COUNT(*) FROM cards WHERE pan = '0000000000000000'");
        assertEquals(count, 0);
    }

    @Test(description = "TC-05 - PATCH и DELETE карты")
    public void tc05_patchAndDeleteCard() throws Exception {
        CardModel createdCard = createTestCard();

        CardModel dbCard = cardFromRow(dbUtils.queryRow("SELECT * FROM cards WHERE pan = ?", createdCard.pan()));
        assertEquals(dbCard.status(), CardModelStatus.ACTIVE);

        httpUtils.httpPatch(
                GATEWAY_URL,
                "/api/cards/" + createdCard.pan(),
                new PatchCardRequest(CardModelStatus.BLOCKED, null, null, null),
                // TODO 204 Status
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

        // TODO разобраться со статусом при получении карты со статусом DELETED
//        httpUtils.httpGet(GATEWAY_URL, "/api/cards/" + createdCard.pan(), 404);
    }

    @Test(description = "TC-15 -  Резервирование средств (POST /api/cards/{pan}/reserve)")
    public void tc15_reserveFunds() throws Exception {
        long firstReserve = 500000L;
        long secondReserve = 300000;
        String firstRrn = "123456789012";
        String secondRrn = "123456789013";

        CardModel createdCard = createTestCard();

        long balanceBefore = dbUtils.queryLong("SELECT available_balance FROM cards WHERE pan = ?", createdCard.pan());
        assertEquals(balanceBefore, createdCard.availableBalance());

        httpUtils.httpPost(GATEWAY_URL, "/api/cards/" + createdCard.pan() + "/reserve",
                new ReserveRequest(firstReserve, firstRrn), 200);

        long balanceAfterFirstReserve = dbUtils.queryLong("SELECT available_balance FROM cards WHERE pan = ?", createdCard.pan());
        assertEquals(balanceAfterFirstReserve, balanceBefore - firstReserve);

        CardModel cardAfterFirstReserve = mapper.treeToValue(
                httpUtils.httpGet(GATEWAY_URL, "/api/cards/" + createdCard.pan(), 200),
                CardModel.class
        );
        assertEquals(cardAfterFirstReserve.availableBalance(), balanceBefore - firstReserve);

        httpUtils.httpPost(GATEWAY_URL, "/api/cards/" + createdCard.pan() + "/reserve",
                new ReserveRequest(secondReserve, secondRrn), 200);

        long balanceAfterSecondReserve = dbUtils.queryLong("SELECT available_balance FROM cards WHERE pan = ?", createdCard.pan());
        assertEquals(balanceAfterSecondReserve, balanceBefore - firstReserve - secondReserve);
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

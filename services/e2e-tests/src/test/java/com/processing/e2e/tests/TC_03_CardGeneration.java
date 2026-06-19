package com.processing.e2e.tests;

import com.processing.common.dto.cardmanagement.CardModel;
import com.processing.common.dto.cardmanagement.GenerateCardResponse;
import com.processing.common.dto.cardmanagement.GenerateCardsRequest;
import com.processing.e2e.E2EBaseTest;
import com.processing.e2e.utility.DBUtils;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.testng.Assert.*;

public class TC_03_CardGeneration extends E2EBaseTest {

    private final DBUtils dbUtils = new DBUtils();

    private static final int COUNT_GENERATE = 10;
    private static final List<String> BINS = List.of("400000", "400001", "400002", "400003", "400004");

    @Test(description = "TC-03 - массовая генерация тестовых карт")
    public void tc03_generateCards() throws Exception {
        GenerateCardsRequest request = new GenerateCardsRequest(COUNT_GENERATE, BINS);
        GenerateCardResponse response = mapper.treeToValue(
                httpUtils.httpPost(GATEWAY_URL, "/api/cards/generate", request, 201),
                GenerateCardResponse.class
        );
        long distinctPansCount = response.cards().stream().distinct().count();

        assertEquals(response.generated(), COUNT_GENERATE, "generated must be: " + COUNT_GENERATE);
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

        List<String> generatedPans = response.cards().stream()
                .map(CardModel::pan)
                .toList();

        long dbSavedCardsCount = dbUtils.queryLong(
                "SELECT COUNT(*) FROM cards WHERE pan IN (" +
                        generatedPans.stream().map(p -> "'" + p + "'").collect(Collectors.joining(",")) +
                        ")"
        );

        assertEquals(dbSavedCardsCount, response.generated(), "db must save all cards");

        List<String> dbBins = dbUtils.queryStringList("SELECT DISTINCT bin FROM cards");
        BINS.forEach(bin -> assertTrue(dbBins.contains(bin), "missing bin in db: " + bin));
    }

}

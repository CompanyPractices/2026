package com.processing.e2e.tests;


import com.fasterxml.jackson.databind.JsonNode;
import com.processing.common.dto.authorization.AuthorizationResponse;
import com.processing.e2e.E2EBaseTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;


import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.oneOf;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;


/**
 * E2E-тесты Switch: TC-06 (полный цикл APPROVED) и TC-14 (маршрутизация 5 BIN → 5 issuerId).
 * <p>
 * Требует поднятый docker-compose (Gateway, Switch, Authorization, Logger, Card Management).
 */
public class TC_06_TC_14_SwitchTest extends E2EBaseTest {


    /** BIN из таблицы маршрутизации Switch. */
    private static final String[] BINS = {"400000", "400001", "400002", "400003", "400004"};
    /** Ожидаемые issuerId для каждого BIN. */
    private static final String[] EXPECTED_ISSUERS = {"ISS001", "ISS002", "ISS003", "ISS004", "ISS005"};


    private static final BigDecimal TC06_AMOUNT = new BigDecimal("150000");
    private static final String TC06_STAN = "000001";
    private static final String TERMINAL_ID = "TERM0001";
    private static final String TERMINAL_TYPE = "POS";
    private static final String MERCHANT_ID = "MERCH0000000001";
    private static final Instant TRANSMISSION_DATE_TIME = Instant.parse("2026-06-01T10:30:00Z");
    private static final Duration STACK_READY_TIMEOUT = Duration.ofMinutes(3);
    private static final Duration STACK_READY_POLL_INTERVAL = Duration.ofSeconds(3);
    /** Сервисы, которые должны быть {@code ok} перед запуском тестов. */
    private static final List<String> REQUIRED_SERVICES =
            List.of("switch", "cardManagement", "authorization", "logger");


    /** STAN транзакций, создаваемых в тестах (для cleanup). */
    private static final String[] TEST_TRANSACTION_STANS = {
            TC06_STAN, "140000", "140001", "140002", "140003", "140004"
    };


    private Connection connection;
    private String tc06Pan;
    private boolean testCardsPrepared;
    private final List<String> generatedPans = new ArrayList<>();


    /**
     * Подключается к БД, очищает артефакты прошлых прогонов, ждёт готовности стека.
     *
     * @throws Exception при ошибке подключения или таймауте ожидания сервисов
     */
    @BeforeClass
    public void setUp() throws Exception {
        RestAssured.baseURI = GATEWAY_URL;
        connection = DriverManager.getConnection(jdbcUrl(), DB_USER, DB_PASSWORD);
        cleanupLeftoverSwitchTestData();
        assertStackIsReady();
    }


    /**
     * Удаляет тестовые транзакции и помечает сгенерированные карты как DELETED.
     *
     * @throws SQLException при ошибке SQL
     */
    @AfterClass(alwaysRun = true)
    public void tearDown() throws SQLException {
        try {
            if (connection != null && !connection.isClosed()) {
                cleanupSwitchTestData();
            }
        } finally {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        }
    }


    /**
     * Опрашивает {@code GET /health} Gateway до готовности всех required-сервисов.
     *
     * @throws Exception при прерывании sleep или ошибке парсинга JSON
     */
    private void assertStackIsReady() throws Exception {
        Instant deadline = Instant.now().plus(STACK_READY_TIMEOUT);
        String lastServicesState = null;


        while (Instant.now().isBefore(deadline)) {
            Response health = given().when().get("/health").then().statusCode(200).extract().response();
            JsonNode body = mapper.readTree(health.asString());
            assertEquals(body.get("status").asText(), "ok", "Gateway must be ok");


            JsonNode services = body.get("services");
            assertNotNull(services, "Gateway health must expose services map");


            List<String> notReady = new ArrayList<>();
            for (String service : REQUIRED_SERVICES) {
                if (!"ok".equals(services.get(service).asText())) {
                    notReady.add(service + "=" + services.get(service).asText());
                }
            }
            if (notReady.isEmpty()) {
                return;
            }


            lastServicesState = String.join(", ", notReady);
            Thread.sleep(STACK_READY_POLL_INTERVAL.toMillis());
        }


        fail("Stack not ready within " + STACK_READY_TIMEOUT.toSeconds()
                + "s. Still down: " + lastServicesState
                + ". Run `docker compose up -d` and wait for all services to become healthy.");
    }


    /**
     * TC-06: транзакция через Gateway → APPROVED, списание баланса, запись в Logger/БД.
     *
     * @throws Exception при ошибках HTTP или SQL
     */
    @Test(description = "TC-06: Полный цикл одиночной транзакции (APPROVED)")
    public void tc06_fullApprovedTransactionCycle() throws Exception {
        ensureTestCards();
        tc06Pan = findActivePanWithBalance(BINS[0], TC06_AMOUNT);
        cleanupTransaction(TC06_STAN);


        long balanceBeforeHttp = getCardBalanceFromApi(tc06Pan);
        long balanceBeforeDb = getCardBalanceFromDb(tc06Pan);
        assertEquals(balanceBeforeDb, balanceBeforeHttp, "HTTP and DB balance before transaction must match");


        Response txResponse = given()
                .contentType(ContentType.JSON)
                .body(transactionPayload(TC06_STAN, tc06Pan, TC06_AMOUNT))
                .when()
                .post("/api/transactions")
                .then()
                .statusCode(200)
                .body("status", equalTo(AuthorizationResponse.STATUS_APPROVED))
                .body("responseCode", equalTo(AuthorizationResponse.CODE_APPROVED))
                .body("mti", equalTo("0100"))
                .body("stan", equalTo(TC06_STAN))
                .body("rrn", matchesPattern("\\d{12}"))
                .body("authCode", matchesPattern("[A-Z0-9]{6}"))
                .body("processingTimeMs", greaterThan(0))
                .extract()
                .response();


        JsonNode txBody = mapper.readTree(txResponse.asString());
        String rrn = txBody.get("rrn").asText();
        String authCode = txBody.get("authCode").asText();


        long balanceAfterDb = getCardBalanceFromDb(tc06Pan);
        assertEquals(balanceAfterDb, balanceBeforeDb - TC06_AMOUNT.longValue(),
                "DB balance must decrease by transaction amount");


        long balanceAfterHttp = getCardBalanceFromApi(tc06Pan);
        assertEquals(balanceAfterHttp, balanceAfterDb,
                "HTTP balance after transaction must match DB");


        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT status, pan, amount, rrn, auth_code, issuer_id, created_at "
                        + "FROM transactions WHERE stan = ?")) {
            stmt.setString(1, TC06_STAN);
            ResultSet rs = stmt.executeQuery();
            assertTrue(rs.next(), "transaction must exist in DB");
            assertEquals(rs.getString("status"), AuthorizationResponse.STATUS_APPROVED);
            assertEquals(rs.getString("pan"), tc06Pan);
            assertEquals(0, rs.getBigDecimal("amount").compareTo(TC06_AMOUNT),
                    "transaction amount must match request");
            assertEquals(rs.getString("rrn"), rrn);
            assertEquals(rs.getString("auth_code"), authCode);
            assertNotNull(rs.getString("issuer_id"));
            assertNotNull(rs.getTimestamp("created_at"));
        }
    }


    /**
     * TC-14: для каждого из 5 BIN issuerId в БД и Search API совпадает с таблицей маршрутизации.
     *
     * @throws Exception при ошибках HTTP или SQL
     */
    @Test(description = "TC-14: Маршрутизация по BIN (5 BIN → 5 issuerId)")
    public void tc14_binRoutingToIssuerId() throws Exception {
        ensureTestCards();
        Set<String> observedIssuers = new HashSet<>();


        for (int i = 0; i < BINS.length; i++) {
            String bin = BINS[i];
            String expectedIssuer = EXPECTED_ISSUERS[i];
            String pan = findActivePanByBin(bin);
            String stan = String.format("14%04d", i);


            cleanupTransaction(stan);


            given()
                    .contentType(ContentType.JSON)
                    .body(transactionPayload(stan, pan, new BigDecimal("1000")))
                    .when()
                    .post("/api/transactions")
                    .then()
                    .statusCode(200)
                    .body("status", oneOf(
                            AuthorizationResponse.STATUS_APPROVED, AuthorizationResponse.STATUS_DECLINED));


            String issuerIdDb = getIssuerIdFromDbByStan(stan);
            assertNotNull(issuerIdDb, "issuer_id must be present in DB for stan " + stan);
            assertEquals(issuerIdDb, expectedIssuer,
                    "BIN " + bin + " must route to " + expectedIssuer);


            String issuerIdHttp = getIssuerIdFromSearchApi(pan, stan);
            assertEquals(issuerIdHttp, issuerIdDb,
                    "issuerId from HTTP search must match DB for pan " + pan);


            observedIssuers.add(issuerIdDb);
        }


        assertEquals(observedIssuers.size(), 5, "must observe 5 distinct issuerId values");
    }


    /**
     * Генерирует 100 тестовых карт по 5 BIN через Card Management (один раз за класс).
     *
     * @throws Exception при ошибке HTTP
     */
    private void ensureTestCards() throws Exception {
        if (testCardsPrepared) {
            return;
        }
        Response response = given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "count": 100,
                          "bins": ["400000","400001","400002","400003","400004"]
                        }
                        """)
                .when()
                .post("/api/cards/generate")
                .then()
                .statusCode(201)
                .extract()
                .response();

        JsonNode cards = mapper.readTree(response.asString()).get("cards");
        for (JsonNode card : cards) {
            generatedPans.add(card.get("pan").asText());
        }
        testCardsPrepared = true;
    }


    /**
     * Очистка перед тестами: транзакции по STAN и soft-delete карт по BIN.
     *
     * @throws SQLException при ошибке SQL
     */
    private void cleanupLeftoverSwitchTestData() throws SQLException {
        deleteTestTransactions();
        softDeleteCardsByBins(BINS);
    }


    /**
     * Очистка после тестов: транзакции и сгенерированные карты.
     *
     * @throws SQLException при ошибке SQL
     */
    private void cleanupSwitchTestData() throws SQLException {
        deleteTestTransactions();
        softDeleteGeneratedCards(generatedPans);
        generatedPans.clear();
    }


    /**
     * Удаляет транзакции с тестовыми STAN из таблицы {@code transactions}.
     *
     * @throws SQLException при ошибке SQL
     */
    private void deleteTestTransactions() throws SQLException {
        for (String stan : TEST_TRANSACTION_STANS) {
            cleanupTransaction(stan);
        }
    }


    /**
     * Помечает сгенерированные карты как DELETED.
     *
     * @param pans список PAN
     * @throws SQLException при ошибке SQL
     */
    private void softDeleteGeneratedCards(List<String> pans) throws SQLException {
        if (pans.isEmpty()) {
            return;
        }
        try (PreparedStatement stmt = connection.prepareStatement(
                "UPDATE cards SET status = 'DELETED' WHERE pan = ? AND status <> 'DELETED'")) {
            for (String pan : pans) {
                stmt.setString(1, pan);
                stmt.executeUpdate();
            }
        }
    }


    /**
     * Помечает все ACTIVE-карты указанных BIN как DELETED.
     *
     * @param bins массив BIN
     * @throws SQLException при ошибке SQL
     */
    private void softDeleteCardsByBins(String[] bins) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(
                "UPDATE cards SET status = 'DELETED' WHERE bin = ? AND status <> 'DELETED'")) {
            for (String bin : bins) {
                stmt.setString(1, bin);
                stmt.executeUpdate();
            }
        }
    }


    /**
     * Формирует JSON-тело запроса транзакции для Gateway.
     *
     * @param stan   STAN операции
     * @param pan    номер карты
     * @param amount сумма
     * @return JSON-строка
     */
    private String transactionPayload(String stan, String pan, BigDecimal amount) {
        return """
                {
                  "mti": "0100",
                  "stan": "%s",
                  "pan": "%s",
                  "processingCode": "000000",
                  "amount": %s,
                  "currencyCode": "643",
                  "transmissionDateTime": "%s",
                  "terminalId": "%s",
                  "terminalType": "%s",
                  "merchantId": "%s",
                  "mcc": "5411",
                  "acquirerId": "ACQ001"
                }
                """.formatted(stan, pan, amount.toPlainString(), TRANSMISSION_DATE_TIME, TERMINAL_ID, TERMINAL_TYPE, MERCHANT_ID);
    }


    /**
     * @param pan номер карты
     * @return availableBalance через Card Management API
     * @throws Exception при ошибке HTTP
     */
    private long getCardBalanceFromApi(String pan) throws Exception {
        Response response = given()
                .queryParam("_", System.currentTimeMillis())
                .when()
                .get("/api/cards/{pan}", pan)
                .then()
                .statusCode(200)
                .extract()
                .response();


        JsonNode body = mapper.readTree(response.asString());
        return body.get("availableBalance").asLong();
    }


    /**
     * @param pan номер карты
     * @return available_balance из таблицы {@code cards}
     * @throws SQLException при ошибке SQL
     */
    private long getCardBalanceFromDb(String pan) throws SQLException {
        return queryLong("SELECT available_balance FROM cards WHERE pan = ?", pan);
    }


    /**
     * @param bin BIN (6 цифр)
     * @return PAN первой ACTIVE-карты с данным BIN
     * @throws SQLException если карта не найдена
     */
    private String findActivePanByBin(String bin) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT pan FROM cards WHERE bin = ? AND status = 'ACTIVE' LIMIT 1")) {
            stmt.setString(1, bin);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("pan");
            }
        }
        throw new IllegalStateException("No ACTIVE card for BIN " + bin);
    }


    /**
     * @param bin        BIN (6 цифр)
     * @param minBalance минимальный баланс
     * @return PAN ACTIVE-карты с достаточным балансом
     * @throws SQLException если карта не найдена
     */
    private String findActivePanWithBalance(String bin, BigDecimal minBalance) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT pan FROM cards WHERE bin = ? AND status = 'ACTIVE' "
                        + "AND available_balance >= ? LIMIT 1")) {
            stmt.setString(1, bin);
            stmt.setBigDecimal(2, minBalance);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("pan");
            }
        }
        throw new IllegalStateException(
                "No ACTIVE card for BIN " + bin + " with balance >= " + minBalance);
    }


    /**
     * @param stan STAN транзакции
     * @return issuer_id из таблицы {@code transactions} или {@code null}
     * @throws SQLException при ошибке SQL
     */
    private String getIssuerIdFromDbByStan(String stan) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT issuer_id FROM transactions WHERE stan = ?")) {
            stmt.setString(1, stan);
            ResultSet rs = stmt.executeQuery();
            return rs.next() ? rs.getString("issuer_id") : null;
        }
    }


    /**
     * @param pan  PAN для поиска
     * @param stan STAN для фильтрации в результатах search
     * @return issuerId из Transaction Logger Search API
     * @throws Exception если транзакция не найдена
     */
    private String getIssuerIdFromSearchApi(String pan, String stan) throws Exception {
        Response response = given()
                .queryParam("pan", pan)
                .queryParam("limit", 50)
                .when()
                .get("/api/transactions/search")
                .then()
                .statusCode(200)
                .extract()
                .response();


        JsonNode transactions = mapper.readTree(response.asString()).get("transactions");
        for (JsonNode tx : transactions) {
            if (stan.equals(tx.get("stan").asText())) {
                return tx.get("issuerId").asText();
            }
        }
        throw new IllegalStateException("Transaction stan " + stan + " not found in search API");
    }


    /**
     * Удаляет транзакцию по STAN (идемпотентная подготовка теста).
     *
     * @param stan STAN операции
     * @throws SQLException при ошибке SQL
     */
    private void cleanupTransaction(String stan) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(
                "DELETE FROM transactions WHERE stan = ?")) {
            stmt.setString(1, stan);
            stmt.executeUpdate();
        }
    }


    /**
     * Выполняет SQL-запрос, возвращающий одно long-значение.
     *
     * @param sql    SQL с плейсхолдерами
     * @param params параметры запроса
     * @return значение первой колонки первой строки
     * @throws SQLException если строк нет или ошибка SQL
     */
    private long queryLong(String sql, Object... params) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            bindParams(stmt, params);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getLong(1);
            }
        }
        throw new IllegalStateException("Query returned no rows: " + sql);
    }


    /**
     * Привязывает параметры к {@link PreparedStatement} по порядку.
     *
     * @param stmt   подготовленный запрос
     * @param params значения параметров
     * @throws SQLException при ошибке setObject
     */
    private void bindParams(PreparedStatement stmt, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            stmt.setObject(i + 1, params[i]);
        }
    }


}

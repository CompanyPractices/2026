package com.processing.cardmanagement.load;

import com.processing.cardmanagement.repositories.CardJpaRepository;
import com.processing.common.dto.cardmanagement.CreateCardRequest;
import io.restassured.http.ContentType;
import net.datafaker.Faker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Locale;
import java.util.concurrent.ExecutionException;

import static io.restassured.RestAssured.given;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public class CardServiceLoadTest {

    private static final String POSTGRES_IMAGE = "postgres:16-alpine";

    @Value("${local.server.port}")
    private int port;

    private final static int MAXIMUM_PARALLEL_REQUESTS = 1_000;
    private final static int MAXIMUM_TOTAL_REQUESTS = 10_000;
    private final LoadTestEngine loadTestEngine =
        new LoadTestEngine(MAXIMUM_PARALLEL_REQUESTS, MAXIMUM_TOTAL_REQUESTS);

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(POSTGRES_IMAGE);

    private final Faker faker = new Faker();

    @Autowired
    private CardJpaRepository cardJpaRepository;

    @AfterEach
    void cleanUp() {
        cardJpaRepository.deleteAll();
    }

    @Test
    void cardServiceCreationLoadTest() throws ExecutionException, InterruptedException {
        loadTestEngine.run(() -> {
            var dailyLimit = faker.number().numberBetween(0L, 15_000_000L);
            var postQuery = new CreateCardRequest(
                faker.number().digits(6),
                faker.name().fullName().toUpperCase(Locale.ROOT),
                faker.number().digits(3),
                dailyLimit,
                faker.number().numberBetween(dailyLimit, 300_000_000L),
                faker.number().numberBetween(0L, 1_000_000L)
            );

            given()
                .contentType(ContentType.JSON)
                .body(postQuery)
                .port(port)
                .when()
                .post("/api/cards")
                .then()
                .statusCode(201);
        }).get();
    }
}

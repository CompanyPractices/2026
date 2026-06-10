package com.processing.cardmanagement.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.processing.cardmanagement.repositories.CardJpaRepository;
import com.processing.cardmanagement.services.LuhnValidator;
import com.processing.common.dto.cardmanagement.GenerateCardsRequest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public class CardGeneratorControllerIntegrationTest {

    private static final String POSTGRES_IMAGE = "postgres:16-alpine";

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LuhnValidator luhnValidator;

    @Autowired
    private CardJpaRepository cardJpaRepository;

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(POSTGRES_IMAGE);

    @Value("${local.server.port}")
    private int port;

    @AfterEach
    void cleanUp() {
        cardJpaRepository.deleteAll();
    }

    @Test
    void generateShouldSaveCardsToDatabaseAndReturn201() throws Exception {
        int count = 100;
        List<String> bins = List.of("400000", "400001");
        GenerateCardsRequest request = new GenerateCardsRequest(count, bins);

        var response = given()
                .contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(request))
                .port(port)
                .when()
                .post("/api/cards/generate")
                .then()
                .statusCode(201)
                .extract()
                .jsonPath();

        int generated = response.getInt("generated");
        assertThat(generated).isEqualTo(100);

        List<String> pans = response.getList("cards.pan", String.class);
        assertThat(pans).doesNotHaveDuplicates();
        pans.forEach(pan -> assertThat(pan)
                .hasSize(16)
                .satisfies(p -> assertThat(luhnValidator.isValid(pan)).isTrue()));

        assertEquals(generated, pans.size());

        List<String> ids = response.getList("cards.pan", String.class);
        ids.forEach(Assertions::assertNotNull);

        List<String> statuses = response.getList("cards.status", String.class);
        statuses.forEach(Assertions::assertNotNull);

        List<String> responseBins = response.getList("cards.bin", String.class);
        bins.forEach(bin -> assertThat(responseBins).contains(bin));

        long dbCount = cardJpaRepository.count();
        assertThat(dbCount).isEqualTo(100);
    }

    @Test
    void generateShouldReturn400WhenCountIsZero() throws Exception {
        GenerateCardsRequest request = new GenerateCardsRequest(0, List.of("400000"));

        given()
                .contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(request))
                .port(port)
                .when()
                .post("/api/cards/generate")
                .then()
                .statusCode(400);
    }
}

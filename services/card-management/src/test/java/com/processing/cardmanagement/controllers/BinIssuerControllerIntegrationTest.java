package com.processing.cardmanagement.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.processing.cardmanagement.models.BinIssuerEntity;
import com.processing.cardmanagement.repositories.BinIssuerJpaRepository;
import com.processing.common.dto.cardmanagement.CreateBinIssuerRequest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public class BinIssuerControllerIntegrationTest {

    private static final String POSTGRES_IMAGE = "postgres:16-alpine";

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BinIssuerJpaRepository repository;

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(POSTGRES_IMAGE);

    @Value("${local.server.port}")
    private int port;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        repository.save(new BinIssuerEntity("400000", "ISS001"));
        repository.save(new BinIssuerEntity("400001", "ISS002"));
    }

    @AfterEach
    void cleanUp() {
        repository.deleteAll();
    }

    @Test
    void getAllShouldReturn200() throws Exception {
        given()
                .port(port)
                .when()
                .get("/api/bins")
                .then()
                .statusCode(200);
    }

    @Test
    void getByBinShouldReturn200AndIssuerId() throws Exception {
        given()
                .port(port)
                .when()
                .get("/api/bins/400000")
                .then()
                .statusCode(200)
                .body("bin", equalTo("400000"))
                .body("issuerId", equalTo("ISS001"));
    }

    @Test
    void getByBinShouldReturn404WhenNotFound() throws Exception {
        given()
                .port(port)
                .when()
                .get("/api/bins/400005")
                .then()
                .statusCode(404);
    }

    @Test
    void createShouldReturn201AndSaveToDb() throws Exception {
        CreateBinIssuerRequest request = new CreateBinIssuerRequest("400002", "ISS003");

        given()
                .contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(request))
                .port(port)
                .when()
                .post("/api/bins")
                .then()
                .statusCode(201);

        assertEquals(3, repository.count());
    }

    @Test
    void createExistedBinShouldReturn409() throws Exception {
        CreateBinIssuerRequest request = new CreateBinIssuerRequest("400000", "ISS001");

        given()
                .contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(request))
                .port(port)
                .when()
                .post("/api/bins")
                .then()
                .statusCode(409);
    }

    @Test
    void deleteShouldRemoveFromDbAndReturn204() {
        given()
                .port(port)
                .when()
                .delete("/api/bins/400000")
                .then()
                .statusCode(204);

        assertEquals(1, repository.count());
    }
}

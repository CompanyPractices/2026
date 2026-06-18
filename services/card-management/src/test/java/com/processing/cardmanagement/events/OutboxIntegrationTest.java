package com.processing.cardmanagement.events;

import com.processing.cardmanagement.models.BinIssuerEntity;
import com.processing.cardmanagement.models.EventStatus;
import com.processing.cardmanagement.models.OutboxEventEntity;
import com.processing.cardmanagement.repositories.BinIssuerJpaRepository;
import com.processing.cardmanagement.repositories.CardJpaRepository;
import com.processing.cardmanagement.repositories.OutboxEventJpaRepository;
import com.processing.common.dto.cardmanagement.CreateCardRequest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public class OutboxIntegrationTest {

    private static final String POSTGRES_IMAGE = "postgres:16-alpine";

    @MockitoBean(name = "cardServiceLogEventListener")
    private CardEventListener mockListener;

    @Autowired
    private CardJpaRepository cardJpaRepository;

    @Autowired
    private OutboxEventJpaRepository outboxRepository;

    @Autowired
    private BinIssuerJpaRepository binIssuerRepository;

    @Autowired
    private OutboxProcessor outboxProcessor;

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(POSTGRES_IMAGE);

    @Value("${local.server.port}")
    private int port;

    private static final CreateCardRequest createCardRequest = new CreateCardRequest(
            "400000",
            "IVAN IVANOV",
            "643",
            new BigDecimal(5_000_000),
            new BigDecimal(15_000_000),
            new BigDecimal(1_000_000)
    );

    @BeforeEach
    void setUp() {
        binIssuerRepository.save(new BinIssuerEntity("400000", "ISS001"));
        binIssuerRepository.save(new BinIssuerEntity("400001", "ISS002"));
    }

    @AfterEach
    void cleanUp() {
        binIssuerRepository.deleteAll();
        outboxRepository.deleteAll();
        cardJpaRepository.deleteAll();
    }

    @Test
    void shouldSaveEventToOutboxWhenCardCreated() {
        given()
                .port(port)
                .contentType(ContentType.JSON)
                .body(createCardRequest)
                .when()
                .post("/api/cards")
                .then()
                .log().all()
                .statusCode(201);

        List<OutboxEventEntity> outboxEvents = outboxRepository.findAll();
        assertEquals(1, outboxEvents.size());
        assertEquals(EventStatus.PENDING.toString(), outboxEvents.getFirst().getStatus());
    }

    @Test
    void shouldSaveEventToOutboxWhenCardCreatedAndMarkAsProcessed() throws InterruptedException {
        given()
                .port(port)
                .contentType(ContentType.JSON)
                .body(createCardRequest)
                .when()
                .post("/api/cards")
                .then()
                .log().all()
                .statusCode(201);

        outboxProcessor.process();

        List<OutboxEventEntity> outboxEvents = outboxRepository.findAll();
        assertEquals(1, outboxEvents.size());
        assertEquals(EventStatus.PROCESSED.toString(), outboxEvents.getFirst().getStatus());
    }

    @Test
    void shouldIncrementRetryCountAndSaveErrorWhenListenerFails() {
        String exceptionMsg = "some exception";
        doThrow(new RuntimeException(exceptionMsg))
                .when(mockListener).onEvent(org.mockito.Mockito.any());

        given()
                .port(port)
                .contentType(ContentType.JSON)
                .body(createCardRequest)
                .when()
                .post("/api/cards")
                .then()
                .statusCode(201);

        outboxProcessor.process();

        List<OutboxEventEntity> outboxEvents = outboxRepository.findAll();
        assertEquals(1, outboxEvents.size());

        OutboxEventEntity failedEvent = outboxEvents.getFirst();
        assertEquals(EventStatus.PENDING.toString(), failedEvent.getStatus());
        assertEquals(1, failedEvent.getRetryCount());
        assertEquals(exceptionMsg, failedEvent.getLastError());
    }

}

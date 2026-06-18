package com.processing.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.processing.SwitchTestData;
import com.processing.common.dto.authorization.AuthorizationResponse;
import com.processing.service.RouteService;
import com.processing.service.RoutingService;
import com.processing.support.CapturingAuthorizationClient;
import com.processing.support.TrackingLoggerClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit-тесты {@link RouteController} через MockMvc.
 */
class RouteControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    /** Собирает standalone MockMvc с тестовыми doubles. */
    @BeforeEach
    void setUp() {
        RouteService routeService = new RouteService(
                new RoutingService(SwitchTestData.defaultProperties()),
                new CapturingAuthorizationClient(),
                (transmissionDateTime, stan, pan, terminalId, amount) -> null,
                new TrackingLoggerClient(true));
        mockMvc = MockMvcBuilders.standaloneSetup(new RouteController(routeService)).build();
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    /** Проверяет {@code POST /api/internal/route} — HTTP 200 и APPROVED в теле. */
    @Test
    void route_returnsAuthorizationResponse() throws Exception {
        mockMvc.perform(post("/api/internal/route")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(SwitchTestData.sampleRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(AuthorizationResponse.STATUS_APPROVED))
                .andExpect(jsonPath("$.responseCode").value(AuthorizationResponse.CODE_APPROVED))
                .andExpect(jsonPath("$.stan").value("000001"));
    }
}

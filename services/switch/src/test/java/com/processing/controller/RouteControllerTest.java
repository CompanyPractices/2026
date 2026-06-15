package com.processing.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.processing.SwitchTestData;
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

class RouteControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

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

    @Test
    void route_returnsAuthorizationResponse() throws Exception {
        mockMvc.perform(post("/api/internal/route")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(SwitchTestData.sampleRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.responseCode").value("00"))
                .andExpect(jsonPath("$.stan").value("000001"));
    }
}

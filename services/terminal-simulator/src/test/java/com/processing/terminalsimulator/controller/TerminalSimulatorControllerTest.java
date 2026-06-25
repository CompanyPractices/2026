package com.processing.terminalsimulator.controller;

import com.processing.common.dto.terminalsimulator.TerminalRunRequest;
import com.processing.common.dto.terminalsimulator.TerminalRunResponse;
import com.processing.common.dto.terminalsimulator.TerminalScenario;
import com.processing.terminalsimulator.service.TerminalSimulatorService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TerminalSimulatorController.class)
class TerminalSimulatorControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TerminalSimulatorService service;

    @Autowired
    private ObjectMapper objectMapper;

    private final int tps = 50;

    @Test
    void run_shouldReturnResponseFromService() throws Exception {
        TerminalRunRequest request = new TerminalRunRequest(10, TerminalScenario.normal, tps);
        TerminalRunResponse expectedResponse = new TerminalRunResponse(10, 8, 2, 123L, null);

        when(service.run(10, TerminalScenario.normal, tps)).thenReturn(expectedResponse);

        mockMvc.perform(post("/api/simulator/terminal/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSubmitted").value(10))
                .andExpect(jsonPath("$.approved").value(8))
                .andExpect(jsonPath("$.declined").value(2));
    }

    @Test
    void run_withInvalidScenario_shouldReturnBadRequest() throws Exception {
        String invalidJson = """
            {
                "count": 5,
                "scenario": "unknown_scenario",
                "tps": 50,
            }
            """;

        mockMvc.perform(post("/api/simulator/terminal/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());

    }
}

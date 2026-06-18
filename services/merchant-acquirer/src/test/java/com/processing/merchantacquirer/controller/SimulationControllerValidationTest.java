package com.processing.merchantacquirer.controller;

import com.processing.merchantacquirer.exception.GlobalExceptionHandler;
import com.processing.merchantacquirer.service.SimulationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;


public class SimulationControllerValidationTest {
    private final SimulationService service = mock(SimulationService.class);
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new SimulationController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private int testPost(String body) throws Exception {
        return mvc.perform(post("/api/simulator/merchant/run").contentType(MediaType.APPLICATION_JSON).content(body)).andReturn().getResponse().getStatus();

    }

    @Test
    void rejectCountBelowOne() throws Exception {
        assertEquals(400, testPost("{\"count\":0,\"scenario\":\"grocery\"}"));
    }

    @Test
    void rejectWithoutScenario() throws Exception {
        assertEquals(400, testPost("{\"count\":1\"}"));
    }

    @Test
    void rejectUnknownScenario() throws Exception {
        assertEquals(400, testPost("{\"count\":1,\"scenario\":\"noname\"}"));
    }

    @Test
    void validRequest() throws Exception {
        assertEquals(200, testPost("{\"count\":1,\"scenario\":\"grocery\"}"));
    }
}

package com.processing.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.processing.e2e.ulility.HttpUtils;
import io.restassured.RestAssured;
import org.testng.annotations.BeforeClass;

public abstract class E2EBaseTest {
    protected static final String GATEWAY_URL       = "";
    protected static final String CARD_MGMT_URL     = "";
    protected static final String SWITCH_URL        = "";
    protected static final String AUTH_URL          = "";
    protected static final String TERMINAL_SIM_URL  = "";
    protected static final String MERCHANT_SIM_URL  = "";
    protected static final String LOGGER_URL        = "";

    protected final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    protected HttpUtils httpUtils;

    @BeforeClass(alwaysRun = true)
    public void baseSetUp() {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        httpUtils = new HttpUtils();
    }

    protected JsonNode httpGet(String baseUrl, String path, int expectedStatus) {
        return httpUtils.httpGet(baseUrl, path, expectedStatus);
    }
}
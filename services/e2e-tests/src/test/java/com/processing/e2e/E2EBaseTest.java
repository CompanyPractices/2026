package com.processing.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.processing.e2e.utility.HttpUtils;
import io.restassured.RestAssured;
import org.testng.annotations.BeforeClass;

public abstract class E2EBaseTest {
    protected static final String GATEWAY_URL       = "http://localhost:8080";
    protected static final String CARD_MGMT_URL     = "http://localhost:8081";
    protected static final String SWITCH_URL        = "http://localhost:8082";
    protected static final String AUTH_URL          = "http://localhost:8083";
    protected static final String TERMINAL_SIM_URL  = "http://localhost:8085";
    protected static final String MERCHANT_SIM_URL  = "http://localhost:8086";
    protected static final String LOGGER_URL        = "http://localhost:8088";
    protected static final String DASHBOARD_URL     = "http://localhost:3000";

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

    protected void assertGetStatus(String baseUrl, String path, int expectedStatus) {
        httpUtils.assertGetStatus(baseUrl, path, expectedStatus);
    }
}

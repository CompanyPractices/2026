package com.processing.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.processing.e2e.ulility.HttpUtils;
import io.restassured.RestAssured;
import org.testng.annotations.BeforeClass;
import com.processing.e2e.ulility.DBUtils;

public abstract class E2EBaseTest {
    protected static final String GATEWAY_URL       = "http://localhost:8080";
    protected static final String CARD_MGMT_URL     = "http://localhost:8081";
    protected static final String SWITCH_URL        = "http://localhost:8082";
    protected static final String AUTH_URL          = "http://localhost:8083";
    protected static final String TERMINAL_SIM_URL  = "http://localhost:8085";
    protected static final String MERCHANT_SIM_URL  = "http://localhost:8086";
    protected static final String LOGGER_URL        = "http://localhost:8088";

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

    protected JsonNode httpPost(String baseUrl, String path, Object body, int expectedStatus) {
        return httpUtils.httpPost(baseUrl, path, body, expectedStatus);
    }

    protected JsonNode httpPostRaw(String baseUrl, String path, String jsonBody, int expectedStatus) {
        return httpUtils.httpPostRaw(baseUrl, path, jsonBody, expectedStatus);
    }

    protected DBUtils db = new DBUtils();
}

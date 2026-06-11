package com.processing.e2e;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.processing.e2e.utility.HttpUtils;
import io.restassured.RestAssured;
import org.testng.annotations.BeforeClass;


public abstract class E2EBaseTest {

    protected static final String GATEWAY_URL = env("GATEWAY_URL", "http://localhost:8080");
    protected static final String CARD_MGMT_URL = env("CARD_MGMT_URL", "http://localhost:8081");
    protected static final String SWITCH_URL = env("SWITCH_URL", "http://localhost:8082");
    protected static final String AUTH_URL = env("AUTH_URL", "http://localhost:8083");
    protected static final String TERMINAL_SIM_URL = env("TERMINAL_SIM_URL", "http://localhost:8085");
    protected static final String MERCHANT_SIM_URL = env("MERCHANT_SIM_URL", "http://localhost:8086");
    protected static final String LOGGER_URL = env("LOGGER_URL", "http://localhost:8088");


    public static final String DB_HOST = env("DB_HOST", "localhost");
    public static final String DB_PORT = env("DB_PORT", "5432");
    public static final String DB_NAME = env("DB_NAME", "smp_db");
    public static final String DB_USER = env("DB_USER", "smp_user");
    public static final String DB_PASSWORD = env("DB_PASSWORD", "smp_password");


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


    public static String jdbcUrl() {
        return "jdbc:postgresql://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME;
    }


    protected static String env(String key, String defaultValue) {
        String value = System.getenv(key);
        return value != null && !value.isBlank() ? value : defaultValue;
    }
}

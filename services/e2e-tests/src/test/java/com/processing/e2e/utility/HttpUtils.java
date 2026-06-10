package com.processing.e2e.utility;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

public class HttpUtils {

    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    public JsonNode httpGet(String baseUrl, String path, int expectedStatus) {
        Response response = RestAssured
                .given()
                .baseUri(baseUrl)
                .when()
                .get(path)
                .then()
                .statusCode(expectedStatus)
                .extract()
                .response();
        return response.body().as(JsonNode.class);
    }

    public JsonNode httpPost(String baseUrl, String path, String jsonBody, int expectedStatus) {
        Response response = RestAssured
                .given()
                    .baseUri(baseUrl)
                    .contentType(ContentType.JSON)
                    .body(jsonBody)
                .when()
                    .post(path)
                .then()
                    .statusCode(expectedStatus)
                    .extract()
                    .response();
        return response.body().as(JsonNode.class);
    }
}

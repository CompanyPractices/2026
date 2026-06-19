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
        try {
            return mapper.readTree(response.asString());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse JSON from " + baseUrl + path, e);
        }
    }

    public JsonNode httpPost(String baseUrl, String path, Object body, int expectedStatus) {
        Response response = RestAssured
                .given()
                    .baseUri(baseUrl)
                    .contentType(ContentType.JSON)
                    .body(body)
                .when()
                    .post(path)
                .then()
                    .statusCode(expectedStatus)
                    .extract()
                    .response();
        return response.body().as(JsonNode.class);
    }

    public JsonNode httpPostRaw(String baseUrl, String path, String jsonBody, int expectedStatus) {
        Response response = RestAssured
                .given()
                .baseUri(baseUrl)
                .contentType("application/json")
                .body(jsonBody)
                .when()
                .post(path)
                .then()
                .statusCode(expectedStatus)
                .extract()
                .response();
        return response.body().as(JsonNode.class);
    }

    public void assertGetStatus(String baseUrl, String path, int expectedStatus) {
        RestAssured
                .given()
                .baseUri(baseUrl)
                .when()
                .get(path)
                .then()
                .statusCode(expectedStatus);
    }

    public JsonNode httpPatchRaw(String baseUrl, String path, String jsonBody, int expectedStatus) {
        Response response = RestAssured
                .given()
                .baseUri(baseUrl)
                .contentType("application/json")
                .body(jsonBody)
                .when()
                .patch(path)
                .then()
                .statusCode(expectedStatus)
                .extract()
                .response();
        return response.body().as(JsonNode.class);
    }

    public void httpPatch(String baseUrl, String path, Object body, int expectedStatus) {
        RestAssured
                .given()
                .baseUri(baseUrl)
                .contentType("application/json")
                .body(body)
                .when()
                .patch(path)
                .then()
                .statusCode(expectedStatus);
    }

    public void httpDelete(String baseUrl, String path, int expectedStatus) {
        RestAssured
                .given()
                .baseUri(baseUrl)
                .when()
                .delete(path)
                .then()
                .statusCode(expectedStatus);
    }
}

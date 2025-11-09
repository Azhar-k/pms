package com.klm.pms.config;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeAll;

/**
 * Base test configuration for REST Assured integration tests.
 * Assumes the application is already running on localhost:8080
 * 
 * This class does NOT start the Spring Boot application.
 * Make sure to start the application manually before running tests:
 *   mvn spring-boot:run
 */
public class TestConfig {

    protected static final String BASE_URL = "http://localhost:8080";
    protected static final String API_BASE_PATH = "/api";
    
    protected static RequestSpecification requestSpec;

    @BeforeAll
    public static void setup() {
        // Configure REST Assured base URI and port
        RestAssured.baseURI = BASE_URL;
        RestAssured.port = 8080;
        RestAssured.basePath = API_BASE_PATH;
        
        // Enable logging for debugging (can be disabled in production tests)
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        
        // Create default request specification
        requestSpec = new RequestSpecBuilder()
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .build();
    }
}


package com.klm.pms.config;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base test configuration for REST Assured integration tests.
 * 
 * Supports Docker containers:
 * - Application runs on port 8081 (mapped from container port 8080)
 * - Database runs on port 5433 (mapped from container port 5432)
 * 
 * Configuration can be overridden via environment variables:
 * - TEST_API_PORT (default: 8081)
 * - TEST_API_HOST (default: localhost)
 */
public class TestConfig {

    private static final Logger logger = LoggerFactory.getLogger(TestConfig.class);
    
    // Default to Docker container ports, can be overridden via environment variables
    protected static final String API_HOST = System.getProperty("test.api.host", 
            System.getenv().getOrDefault("TEST_API_HOST", "localhost"));
    protected static final int API_PORT = Integer.parseInt(
            System.getProperty("test.api.port", 
                    System.getenv().getOrDefault("TEST_API_PORT", "8081")));
    
    protected static final String BASE_URL = "http://" + API_HOST + ":" + API_PORT;
    protected static final String API_BASE_PATH = "/api";
    
    protected static RequestSpecification requestSpec;

    @BeforeAll
    public static void setup() {
        logger.info("Configuring REST Assured for tests");
        logger.info("API Base URL: {}", BASE_URL);
        logger.info("API Port: {}", API_PORT);
        
        // Configure REST Assured base URI and port
        RestAssured.baseURI = BASE_URL;
        RestAssured.port = API_PORT;
        RestAssured.basePath = API_BASE_PATH;
        
        // Enable logging for debugging (can be disabled in production tests)
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        
        // Create default request specification
        requestSpec = new RequestSpecBuilder()
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .build();
        
        logger.info("REST Assured configuration completed");
    }
}


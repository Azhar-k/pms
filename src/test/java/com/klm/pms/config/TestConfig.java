package com.klm.pms.config;

import com.klm.pms.util.TestJwtTokenGenerator;
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
 * 
 * Includes JWT token generation for authenticated requests.
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
    
    // Default test user for generating tokens
    protected static final String DEFAULT_TEST_USER = "test_user";
    
    // Generated test token (valid for 1 hour)
    protected static String testToken;
    
    protected static RequestSpecification requestSpec;
    protected static RequestSpecification authenticatedRequestSpec;

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
        
        // Generate test JWT token
        testToken = TestJwtTokenGenerator.generateToken(DEFAULT_TEST_USER);
        logger.info("Generated test JWT token for user: {}", DEFAULT_TEST_USER);
        
        // Create default request specification (without authentication)
        requestSpec = new RequestSpecBuilder()
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .build();
        
        // Create authenticated request specification (with JWT token)
        authenticatedRequestSpec = new RequestSpecBuilder()
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .addHeader("Authorization", "Bearer " + testToken)
                .build();
        
        logger.info("REST Assured configuration completed");
    }
    
    /**
     * Get a valid test token for a specific user.
     * 
     * @param username The username to generate a token for
     * @return A valid JWT token string
     */
    protected static String getTestToken(String username) {
        return TestJwtTokenGenerator.generateToken(username);
    }
    
    /**
     * Get the default test token.
     * 
     * @return The default test token
     */
    protected static String getTestToken() {
        return testToken;
    }
    
    /**
     * Get an expired test token for testing expiration scenarios.
     * 
     * @param username The username to generate an expired token for
     * @return An expired JWT token string
     */
    protected static String getExpiredToken(String username) {
        return TestJwtTokenGenerator.generateExpiredToken(username);
    }
    
    /**
     * Get an invalid test token for testing invalid token scenarios.
     * 
     * @param username The username to generate an invalid token for
     * @return An invalid JWT token string
     */
    protected static String getInvalidToken(String username) {
        return TestJwtTokenGenerator.generateInvalidToken(username);
    }
}


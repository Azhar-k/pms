package com.klm.pms.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Properties;

/**
 * Utility class for generating JWT tokens for integration tests.
 * Uses the same secret key as the application to generate valid tokens.
 * Reads the secret from application.properties or application-test.properties.
 */
public class TestJwtTokenGenerator {

    // Default secret key (fallback if not found in properties)
    private static final String FALLBACK_SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    
    // Default token expiration: 1 hour
    private static final long DEFAULT_EXPIRATION_MS = 3600_000L;
    
    // Cache the secret key after first read
    private static String cachedSecret = null;

    /**
     * Get the JWT secret from application properties.
     * Checks environment variable first, then application-test.properties, then application.properties.
     * 
     * @return The JWT secret key
     */
    private static String getSecretFromProperties() {
        // Return cached value if already loaded
        if (cachedSecret != null) {
            return cachedSecret;
        }
        
        // Check environment variable first (matches application.properties pattern)
        String envSecret = System.getenv("JWT_SECRET");
        if (envSecret != null && !envSecret.isEmpty()) {
            cachedSecret = envSecret;
            return cachedSecret;
        }
        
        // Try to read from application-test.properties first (test profile)
        String secret = readSecretFromProperties("application-test.properties");
        if (secret != null) {
            cachedSecret = secret;
            return cachedSecret;
        }
        
        // Fall back to application.properties
        secret = readSecretFromProperties("application.properties");
        if (secret != null) {
            cachedSecret = secret;
            return cachedSecret;
        }
        
        // Use fallback if nothing found
        cachedSecret = FALLBACK_SECRET;
        return cachedSecret;
    }
    
    /**
     * Read JWT secret from a properties file.
     * 
     * @param propertiesFileName The name of the properties file
     * @return The JWT secret, or null if not found
     */
    private static String readSecretFromProperties(String propertiesFileName) {
        try (InputStream inputStream = TestJwtTokenGenerator.class.getClassLoader()
                .getResourceAsStream(propertiesFileName)) {
            
            if (inputStream == null) {
                return null;
            }
            
            Properties properties = new Properties();
            properties.load(inputStream);
            
            // Check for jwt.secret property
            String secret = properties.getProperty("jwt.secret");
            if (secret != null && !secret.isEmpty()) {
                // Handle environment variable syntax: ${JWT_SECRET:default}
                if (secret.startsWith("${") && secret.contains(":")) {
                    // Extract default value after the colon
                    int colonIndex = secret.lastIndexOf(":");
                    int endIndex = secret.lastIndexOf("}");
                    if (colonIndex > 0 && endIndex > colonIndex) {
                        return secret.substring(colonIndex + 1, endIndex);
                    }
                }
                return secret;
            }
        } catch (Exception e) {
            // Ignore and return null to try next source
        }
        return null;
    }
    
    /**
     * Get the default secret key (reads from properties or uses fallback).
     * 
     * @return The default secret key
     */
    private static String getDefaultSecret() {
        return getSecretFromProperties();
    }

    /**
     * Generate a valid JWT token for testing.
     * Uses the secret key from application.properties.
     * 
     * @param username The username (subject) to include in the token
     * @return A valid JWT token string
     */
    public static String generateToken(String username) {
        return generateToken(username, getDefaultSecret(), DEFAULT_EXPIRATION_MS);
    }

    /**
     * Generate a valid JWT token for testing with custom expiration.
     * Uses the secret key from application.properties.
     * 
     * @param username The username (subject) to include in the token
     * @param expirationMs Token expiration in milliseconds from now
     * @return A valid JWT token string
     */
    public static String generateToken(String username, long expirationMs) {
        return generateToken(username, getDefaultSecret(), expirationMs);
    }

    /**
     * Generate a valid JWT token for testing with custom secret and expiration.
     * 
     * @param username The username (subject) to include in the token
     * @param secret The secret key to sign the token
     * @param expirationMs Token expiration in milliseconds from now
     * @return A valid JWT token string
     */
    public static String generateToken(String username, String secret, long expirationMs) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationMs);

        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

        return Jwts.builder()
                .subject(username)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(key)
                .compact();
    }

    /**
     * Generate an expired JWT token for testing expiration scenarios.
     * Uses the secret key from application.properties.
     * 
     * @param username The username (subject) to include in the token
     * @return An expired JWT token string
     */
    public static String generateExpiredToken(String username) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() - 1000); // Expired 1 second ago

        SecretKey key = Keys.hmacShaKeyFor(getDefaultSecret().getBytes(StandardCharsets.UTF_8));

        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date(now.getTime() - 3600_000L)) // Issued 1 hour ago
                .expiration(expiration)
                .signWith(key)
                .compact();
    }

    /**
     * Generate an invalid JWT token (signed with wrong secret) for testing.
     * 
     * @param username The username (subject) to include in the token
     * @return An invalid JWT token string
     */
    public static String generateInvalidToken(String username) {
        String wrongSecret = "WRONG_SECRET_KEY_FOR_TESTING_PURPOSES_ONLY";
        return generateToken(username, wrongSecret, DEFAULT_EXPIRATION_MS);
    }
}


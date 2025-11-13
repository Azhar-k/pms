package com.klm.pms.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Utility class for generating JWT tokens for integration tests.
 * Uses the same secret key as the application to generate valid tokens.
 */
public class TestJwtTokenGenerator {

    // Default secret key (should match application.properties)
    private static final String DEFAULT_SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    
    // Default token expiration: 1 hour
    private static final long DEFAULT_EXPIRATION_MS = 3600_000L;

    /**
     * Generate a valid JWT token for testing.
     * 
     * @param username The username (subject) to include in the token
     * @return A valid JWT token string
     */
    public static String generateToken(String username) {
        return generateToken(username, DEFAULT_SECRET, DEFAULT_EXPIRATION_MS);
    }

    /**
     * Generate a valid JWT token for testing with custom expiration.
     * 
     * @param username The username (subject) to include in the token
     * @param expirationMs Token expiration in milliseconds from now
     * @return A valid JWT token string
     */
    public static String generateToken(String username, long expirationMs) {
        return generateToken(username, DEFAULT_SECRET, expirationMs);
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
     * 
     * @param username The username (subject) to include in the token
     * @return An expired JWT token string
     */
    public static String generateExpiredToken(String username) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() - 1000); // Expired 1 second ago

        SecretKey key = Keys.hmacShaKeyFor(DEFAULT_SECRET.getBytes(StandardCharsets.UTF_8));

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


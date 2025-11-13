package com.klm.pms.security;

import com.klm.pms.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor to validate JWT tokens for all API requests.
 * Validates the Authorization header and extracts the username from the token.
 */
@Component
public class JwtAuthenticationInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationInterceptor.class);
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String USERNAME_ATTRIBUTE = "username";

    @Autowired
    private JwtTokenValidator jwtTokenValidator;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Skip authentication for Swagger/OpenAPI endpoints
        String requestPath = request.getRequestURI();
        if (isPublicPath(requestPath)) {
            logger.debug("Skipping authentication for public path: {}", requestPath);
            return true;
        }

        // Extract Authorization header
        String authorizationHeader = request.getHeader(AUTHORIZATION_HEADER);
        
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            logger.warn("Missing Authorization header for request: {}", requestPath);
            throw new UnauthorizedException("Missing Authorization header");
        }

        // Validate Bearer token format
        String normalizedHeader = authorizationHeader.trim();
        if (!normalizedHeader.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            logger.warn("Invalid Authorization header format for request: {}", requestPath);
            throw new UnauthorizedException("Authorization header must start with Bearer");
        }

        // Extract token
        String token = normalizedHeader.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty()) {
            logger.warn("Bearer token is empty for request: {}", requestPath);
            throw new UnauthorizedException("Bearer token is missing");
        }

        // Validate token
        TokenValidationResult validationResult = jwtTokenValidator.validateTokenWithDetails(token);
        if (!validationResult.isValid()) {
            if (validationResult.isExpired()) {
                logger.warn("Expired token for request: {}", requestPath);
                throw new UnauthorizedException("Token has expired");
            }
            String errorMessage = validationResult.getError() != null
                    ? "Invalid token: " + validationResult.getError()
                    : "Invalid token";
            logger.warn("Invalid token for request: {} - {}", requestPath, errorMessage);
            throw new UnauthorizedException(errorMessage);
        }

        // Extract and validate username
        String username = validationResult.getUsername();
        if (username == null || username.isBlank()) {
            logger.warn("Token missing subject claim for request: {}", requestPath);
            throw new UnauthorizedException("Token is missing the required subject claim");
        }

        // Store username in request attribute for use in controllers
        request.setAttribute(USERNAME_ATTRIBUTE, username);
        logger.debug("Token successfully validated for user '{}' on path: {}", username, requestPath);
        
        return true;
    }

    /**
     * Check if the request path is public (doesn't require authentication).
     * Excludes Swagger/OpenAPI endpoints and static resources.
     */
    private boolean isPublicPath(String path) {
        return path.startsWith("/swagger-ui") ||
               path.startsWith("/api-docs") ||
               path.startsWith("/v3/api-docs") ||
               path.startsWith("/swagger-ui.html") ||
               path.startsWith("/webjars") ||
               path.startsWith("/favicon.ico") ||
               path.equals("/");
    }
}


package com.klm.pms.security;

import com.klm.pms.exception.UnauthorizedException;
import com.klm.pms.util.SecurityContextUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

/**
 * Interceptor to enforce role-based access control based on @RequireRole annotation.
 * Checks if the current user has the required roles before allowing access to an endpoint.
 * 
 * If @RequireRole is not specified, all authenticated users are allowed.
 */
@Component
public class RoleBasedAccessInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(RoleBasedAccessInterceptor.class);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Only process handler methods (not static resources, etc.)
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;
        Method method = handlerMethod.getMethod();
        Class<?> controllerClass = handlerMethod.getBeanType();

        // Check for @RequireRole annotation on the method first, then on the class
        RequireRole requireRole = method.getAnnotation(RequireRole.class);
        if (requireRole == null) {
            requireRole = controllerClass.getAnnotation(RequireRole.class);
        }

        // If no @RequireRole annotation, allow all authenticated users
        if (requireRole == null) {
            logger.debug("No @RequireRole annotation found for {} - allowing all authenticated users", 
                    request.getRequestURI());
            return true;
        }

        // Get required roles from annotation
        String[] requiredRoles = requireRole.value();
        if (requiredRoles == null || requiredRoles.length == 0) {
            // Empty roles array means allow all authenticated users
            logger.debug("@RequireRole with empty roles for {} - allowing all authenticated users", 
                    request.getRequestURI());
            return true;
        }

        // Get current user's roles
        List<String> userRoles = SecurityContextUtil.getCurrentUserRoles();
        String username = SecurityContextUtil.getCurrentUsername();

        if (userRoles == null || userRoles.isEmpty()) {
            logger.warn("User '{}' attempted to access {} but has no roles", username, request.getRequestURI());
            throw new UnauthorizedException("Access denied: User has no roles assigned");
        }

        // Check if user has at least one of the required roles
        boolean hasRequiredRole = Arrays.stream(requiredRoles)
                .anyMatch(requiredRoleName -> hasRole(userRoles, requiredRoleName));

        if (!hasRequiredRole) {
            logger.warn("User '{}' with roles {} attempted to access {} which requires one of: {}", 
                    username, userRoles, request.getRequestURI(), Arrays.toString(requiredRoles));
            throw new UnauthorizedException(
                    String.format("Access denied: This endpoint requires one of the following roles: %s", 
                            String.join(", ", requiredRoles)));
        }

        logger.debug("User '{}' with roles {} granted access to {} (required: {})", 
                username, userRoles, request.getRequestURI(), Arrays.toString(requiredRoles));
        return true;
    }

    /**
     * Check if the user has a specific role, handling various role name formats.
     * Supports formats like: "admin", "ADMIN", "ROLE_ADMIN", "role_admin"
     * 
     * @param userRoles List of user's roles
     * @param requiredRole The required role name
     * @return true if user has the role, false otherwise
     */
    private boolean hasRole(List<String> userRoles, String requiredRole) {
        // Normalize role names for comparison (case-insensitive, remove ROLE_ prefix if present)
        String normalizedRequiredRole = normalizeRole(requiredRole);
        
        return userRoles.stream()
                .map(this::normalizeRole)
                .anyMatch(userRole -> userRole.equals(normalizedRequiredRole));
    }

    /**
     * Normalize a role name by converting to lowercase and removing ROLE_ prefix if present.
     * 
     * @param role The role name to normalize
     * @return Normalized role name (lowercase, without ROLE_ prefix)
     */
    private String normalizeRole(String role) {
        if (role == null) {
            return "";
        }
        String normalized = role.trim().toLowerCase();
        // Remove ROLE_ prefix if present
        if (normalized.startsWith("role_")) {
            normalized = normalized.substring(5); // Remove "role_" prefix
        }
        return normalized;
    }
}


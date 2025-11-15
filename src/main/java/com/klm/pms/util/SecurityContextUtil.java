package com.klm.pms.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

/**
 * Utility class to access security context information from request attributes.
 * Used to get current user and roles from JWT token validation.
 */
public class SecurityContextUtil {

    private static final String USERNAME_ATTRIBUTE = "username";
    private static final String ROLES_ATTRIBUTE = "roles";

    /**
     * Get the current username from the request context.
     * 
     * @return The username, or null if not available
     */
    public static String getCurrentUsername() {
        HttpServletRequest request = getCurrentRequest();
        if (request != null) {
            Object username = request.getAttribute(USERNAME_ATTRIBUTE);
            return username != null ? username.toString() : null;
        }
        return null;
    }

    /**
     * Get the current user roles from the request context.
     * 
     * @return The list of roles, or null if not available
     */
    @SuppressWarnings("unchecked")
    public static List<String> getCurrentUserRoles() {
        HttpServletRequest request = getCurrentRequest();
        if (request != null) {
            Object roles = request.getAttribute(ROLES_ATTRIBUTE);
            return roles instanceof List ? (List<String>) roles : null;
        }
        return null;
    }

    /**
     * Check if the current user has a specific role.
     * 
     * @param role The role to check
     * @return true if the user has the role, false otherwise
     */
    public static boolean hasRole(String role) {
        List<String> roles = getCurrentUserRoles();
        return roles != null && roles.contains(role);
    }

    /**
     * Check if the current user is an admin.
     * 
     * @return true if the user has ADMIN role, false otherwise
     */
    public static boolean isAdmin() {
        return hasRole("ADMIN") || hasRole("admin") || hasRole("ROLE_ADMIN");
    }

    /**
     * Get the current HTTP request from the request context.
     * 
     * @return The current HttpServletRequest, or null if not available
     */
    private static HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }
}


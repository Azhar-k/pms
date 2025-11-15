package com.klm.pms.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to specify required roles for accessing an endpoint.
 * If not specified, all authenticated users are allowed.
 * 
 * Usage:
 * - @RequireRole("admin") - requires admin role
 * - @RequireRole({"admin", "manager"}) - requires admin OR manager role
 * - No annotation - allows all authenticated users
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {
    /**
     * The roles required to access this endpoint.
     * If multiple roles are specified, the user needs to have at least one of them.
     * 
     * @return Array of required role names
     */
    String[] value() default {};
}


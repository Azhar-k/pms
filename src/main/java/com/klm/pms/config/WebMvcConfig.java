package com.klm.pms.config;

import com.klm.pms.security.JwtAuthenticationInterceptor;
import com.klm.pms.security.RoleBasedAccessInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuration class to register JWT authentication and role-based access interceptors.
 * The interceptors will validate JWT tokens and enforce role-based access control for all API requests.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private JwtAuthenticationInterceptor jwtAuthenticationInterceptor;

    @Autowired
    private RoleBasedAccessInterceptor roleBasedAccessInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Register JWT authentication interceptor first (runs before role check)
        registry.addInterceptor(jwtAuthenticationInterceptor)
                .addPathPatterns("/api/**")  // Apply to all API endpoints
                .excludePathPatterns(
                        "/swagger-ui/**",
                        "/api-docs/**",
                        "/v3/api-docs/**",
                        "/swagger-ui.html",
                        "/webjars/**",
                        "/health/**"  // Exclude health check endpoint
                );  // Exclude Swagger/OpenAPI endpoints and health checks

        // Register role-based access interceptor (runs after authentication)
        registry.addInterceptor(roleBasedAccessInterceptor)
                .addPathPatterns("/api/**")  // Apply to all API endpoints
                .excludePathPatterns(
                        "/swagger-ui/**",
                        "/api-docs/**",
                        "/v3/api-docs/**",
                        "/swagger-ui.html",
                        "/webjars/**",
                        "/health/**"  // Exclude health check endpoint
                );  // Exclude Swagger/OpenAPI endpoints and health checks
    }
}


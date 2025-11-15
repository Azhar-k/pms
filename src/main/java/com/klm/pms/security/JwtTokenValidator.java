package com.klm.pms.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class JwtTokenValidator {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenValidator.class);

    @Value("${jwt.secret}")
    private String secretKey;

    private SecretKey getSigningKey() {
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public TokenValidationResult validateTokenWithDetails(String token) {
        try {
            Claims claims = extractAllClaims(token);
            String username = claims.getSubject();
            Date expiration = claims.getExpiration();
            boolean expired = expiration != null && expiration.before(new Date());

            // Extract roles from token claims
            List<String> roles = extractRoles(claims);

            return TokenValidationResult.builder()
                    .valid(!expired)
                    .username(username)
                    .expired(expired)
                    .roles(roles)
                    .build();
        } catch (Exception ex) {
            logger.warn("Token validation failed", ex);
            return TokenValidationResult.builder()
                    .valid(false)
                    .error(ex.getMessage())
                    .build();
        }
    }

    private List<String> extractRoles(Claims claims) {
        List<String> roles = new ArrayList<>();
        
        // Try to extract roles from "roles" claim (array or list)
        Object rolesClaim = claims.get("roles");
        if (rolesClaim != null) {
            if (rolesClaim instanceof List) {
                for (Object role : (List<?>) rolesClaim) {
                    if (role != null) {
                        roles.add(role.toString());
                    }
                }
            } else if (rolesClaim instanceof String) {
                roles.add((String) rolesClaim);
            }
        }
        
        // Try to extract role from "role" claim (single value)
        Object roleClaim = claims.get("role");
        if (roleClaim != null) {
            roles.add(roleClaim.toString());
        }
        
        return roles;
    }
}


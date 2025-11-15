package com.klm.pms.security;

import java.util.List;

public class TokenValidationResult {

    private final boolean valid;
    private final String username;
    private final boolean expired;
    private final String error;
    private final List<String> roles;

    private TokenValidationResult(Builder builder) {
        this.valid = builder.valid;
        this.username = builder.username;
        this.expired = builder.expired;
        this.error = builder.error;
        this.roles = builder.roles;
    }

    public boolean isValid() {
        return valid;
    }

    public String getUsername() {
        return username;
    }

    public boolean isExpired() {
        return expired;
    }

    public String getError() {
        return error;
    }

    public List<String> getRoles() {
        return roles;
    }

    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private boolean valid;
        private String username;
        private boolean expired;
        private String error;
        private List<String> roles;

        private Builder() {
        }

        public Builder valid(boolean valid) {
            this.valid = valid;
            return this;
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder expired(boolean expired) {
            this.expired = expired;
            return this;
        }

        public Builder error(String error) {
            this.error = error;
            return this;
        }

        public Builder roles(List<String> roles) {
            this.roles = roles;
            return this;
        }

        public TokenValidationResult build() {
            return new TokenValidationResult(this);
        }
    }
}


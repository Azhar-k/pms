package com.klm.pms.security;

import java.util.Date;

public class TokenValidationResult {

    private final boolean valid;
    private final String username;
    private final boolean expired;
    private final Date expiresAt;
    private final String error;

    private TokenValidationResult(Builder builder) {
        this.valid = builder.valid;
        this.username = builder.username;
        this.expired = builder.expired;
        this.expiresAt = builder.expiresAt;
        this.error = builder.error;
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

    public Date getExpiresAt() {
        return expiresAt;
    }

    public String getError() {
        return error;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private boolean valid;
        private String username;
        private boolean expired;
        private Date expiresAt;
        private String error;

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

        public Builder expiresAt(Date expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }

        public Builder error(String error) {
            this.error = error;
            return this;
        }

        public TokenValidationResult build() {
            return new TokenValidationResult(this);
        }
    }
}


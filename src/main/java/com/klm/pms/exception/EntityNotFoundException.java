package com.klm.pms.exception;

/**
 * Exception thrown when a requested entity is not found in the system.
 */
public class EntityNotFoundException extends RuntimeException {

    private final String entityType;
    private final Object identifier;

    public EntityNotFoundException(String entityType, Object identifier) {
        super(String.format("%s not found with identifier: %s", entityType, identifier));
        this.entityType = entityType;
        this.identifier = identifier;
    }

    public EntityNotFoundException(String entityType, Object identifier, String message) {
        super(message);
        this.entityType = entityType;
        this.identifier = identifier;
    }

    public String getEntityType() {
        return entityType;
    }

    public Object getIdentifier() {
        return identifier;
    }
}


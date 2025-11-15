package com.klm.pms.exception;

/**
 * Exception thrown when attempting to create an entity that already exists.
 */
public class DuplicateEntityException extends RuntimeException {

    private final String entityType;
    private final String fieldName;
    private final Object fieldValue;

    public DuplicateEntityException(String entityType, String fieldName, Object fieldValue) {
        super(String.format("%s with %s '%s' already exists", entityType, fieldName, fieldValue));
        this.entityType = entityType;
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
    }

    public String getEntityType() {
        return entityType;
    }

    public String getFieldName() {
        return fieldName;
    }

    public Object getFieldValue() {
        return fieldValue;
    }
}


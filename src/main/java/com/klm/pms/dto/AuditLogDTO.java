package com.klm.pms.dto;

import com.klm.pms.model.AuditLog;

import java.time.LocalDateTime;

public class AuditLogDTO {

    private Long id;
    private String entityType;
    private Long entityId;
    private AuditLog.AuditAction action;
    private String username;
    private LocalDateTime timestamp;
    private String oldValue;
    private String newValue;
    private String description;
    private String requestPath;
    private String requestMethod;

    // Constructors
    public AuditLogDTO() {
    }

    public AuditLogDTO(Long id, String entityType, Long entityId, AuditLog.AuditAction action,
                      String username, LocalDateTime timestamp, String description) {
        this.id = id;
        this.entityType = entityType;
        this.entityId = entityId;
        this.action = action;
        this.username = username;
        this.timestamp = timestamp;
        this.description = description;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public Long getEntityId() {
        return entityId;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }

    public AuditLog.AuditAction getAction() {
        return action;
    }

    public void setAction(AuditLog.AuditAction action) {
        this.action = action;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getOldValue() {
        return oldValue;
    }

    public void setOldValue(String oldValue) {
        this.oldValue = oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRequestPath() {
        return requestPath;
    }

    public void setRequestPath(String requestPath) {
        this.requestPath = requestPath;
    }

    public String getRequestMethod() {
        return requestMethod;
    }

    public void setRequestMethod(String requestMethod) {
        this.requestMethod = requestMethod;
    }
}


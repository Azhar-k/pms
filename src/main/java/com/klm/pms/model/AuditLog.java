package com.klm.pms.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_audit_entity_type", columnList = "entityType"),
    @Index(name = "idx_audit_entity_id", columnList = "entityId"),
    @Index(name = "idx_audit_username", columnList = "username"),
    @Index(name = "idx_audit_timestamp", columnList = "timestamp")
})
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String entityType; // e.g., "Guest", "Room", "Reservation"

    @Column(nullable = false)
    private Long entityId; // ID of the entity being audited

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AuditAction action; // CREATE, UPDATE, DELETE

    @Column(nullable = false)
    private String username; // User who performed the action

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(columnDefinition = "TEXT")
    private String changes; // JSON representation of changed fields (for UPDATE operations)

    @Column(length = 500)
    private String description; // Human-readable description

    @Column(length = 1000)
    private String requestPath; // API endpoint that triggered the action

    @Column(length = 50)
    private String requestMethod; // HTTP method (GET, POST, PUT, DELETE)

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }

    // Constructors
    public AuditLog() {
    }

    public AuditLog(String entityType, Long entityId, AuditAction action, String username) {
        this.entityType = entityType;
        this.entityId = entityId;
        this.action = action;
        this.username = username;
        this.timestamp = LocalDateTime.now();
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

    public AuditAction getAction() {
        return action;
    }

    public void setAction(AuditAction action) {
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

    public String getChanges() {
        return changes;
    }

    public void setChanges(String changes) {
        this.changes = changes;
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

    public enum AuditAction {
        CREATE,
        UPDATE,
        DELETE
    }
}


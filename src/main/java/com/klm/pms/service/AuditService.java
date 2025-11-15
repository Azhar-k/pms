package com.klm.pms.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.klm.pms.model.AuditLog;
import com.klm.pms.repository.AuditLogRepository;
import com.klm.pms.util.SecurityContextUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for handling audit logging of all write operations.
 */
@Service
public class AuditService {

    private static final Logger logger = LoggerFactory.getLogger(AuditService.class);

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Log a CREATE operation.
     * Note: For CREATE operations, we don't capture the new value as per requirements.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logCreate(String entityType, Long entityId, Object entity) {
        logOperation(AuditLog.AuditAction.CREATE, entityType, entityId, null, null);
    }

    /**
     * Log an UPDATE operation.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logUpdate(String entityType, Long entityId, Object oldEntity, Object newEntity) {
        logOperation(AuditLog.AuditAction.UPDATE, entityType, entityId, oldEntity, newEntity);
    }

    /**
     * Log a DELETE operation.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logDelete(String entityType, Long entityId, Object entity) {
        logOperation(AuditLog.AuditAction.DELETE, entityType, entityId, entity, null);
    }

    /**
     * Generic method to log any audit operation.
     */
    private void logOperation(AuditLog.AuditAction action, String entityType, Long entityId, 
                              Object oldEntity, Object newEntity) {
        logger.debug("Starting audit log operation: {} {} {} (entityId: {})", action, entityType, entityId, entityId);
        
        try {
            String username = SecurityContextUtil.getCurrentUsername();
            logger.debug("Current username from security context: {}", username);
            
            if (username == null) {
                username = "SYSTEM"; // Fallback for system operations
                logger.warn("No username found in security context, using SYSTEM as fallback");
            }

            AuditLog auditLog = new AuditLog();
            auditLog.setEntityType(entityType);
            auditLog.setEntityId(entityId);
            auditLog.setAction(action);
            auditLog.setUsername(username);
            auditLog.setTimestamp(LocalDateTime.now());
            
            logger.debug("Created audit log object for {} {} {} by user {}", action, entityType, entityId, username);

            // Handle entity state serialization based on action type
            if (action == AuditLog.AuditAction.CREATE) {
                // For CREATE operations, don't capture changes
                logger.debug("CREATE operation: Skipping changes capture for {} {}", entityType, entityId);
                auditLog.setChanges(null);
            } else if (action == AuditLog.AuditAction.UPDATE) {
                // For UPDATE operations, capture only changed fields
                logger.debug("UPDATE operation: Comparing old and new entity states for {} {}", entityType, entityId);
                if (oldEntity != null && newEntity != null) {
                    try {
                        // Compare and capture only changed fields
                        Map<String, Object> changedFields = getChangedFields(oldEntity, newEntity);
                        if (!changedFields.isEmpty()) {
                            String changedFieldsJson = objectMapper.writeValueAsString(changedFields);
                            auditLog.setChanges(changedFieldsJson);
                            logger.debug("Captured {} changed field(s) for {} {}: {}", 
                                    changedFields.size(), entityType, entityId, changedFields.keySet());
                        } else {
                            auditLog.setChanges(null);
                            logger.debug("No fields changed for {} {}", entityType, entityId);
                        }
                    } catch (Exception e) {
                        logger.warn("Failed to compare entity states for audit log", e);
                        auditLog.setChanges(null);
                    }
                } else {
                    logger.warn("UPDATE operation missing old or new entity for {} {}", entityType, entityId);
                    auditLog.setChanges(null);
                }
            } else if (action == AuditLog.AuditAction.DELETE) {
                // For DELETE operations, don't capture changes
                logger.debug("DELETE operation: Skipping changes capture for {} {}", entityType, entityId);
                auditLog.setChanges(null);
            }

            // Get request information if available
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                auditLog.setRequestPath(request.getRequestURI());
                auditLog.setRequestMethod(request.getMethod());
                logger.debug("Captured request info: {} {}", request.getMethod(), request.getRequestURI());
            } else {
                logger.debug("No request attributes available (might be called outside HTTP request context)");
            }

            // Create description
            auditLog.setDescription(String.format("%s %s with ID %d", action, entityType, entityId));

            logger.debug("Saving audit log to database: {} {} {} by {}", action, entityType, entityId, username);
            AuditLog savedAuditLog = auditLogRepository.save(auditLog);
            logger.info("Successfully created audit log with ID {}: {} {} {} by user {}", 
                    savedAuditLog.getId(), action, entityType, entityId, username);
        } catch (Exception e) {
            // Don't let audit logging failures break the main operation
            logger.error("Failed to create audit log for {} {} {} (entityId: {}): {}", 
                    action, entityType, entityId, entityId, e.getMessage(), e);
            logger.error("Exception details:", e);
        }
    }

    /**
     * Compares two entities and returns a map containing only the changed fields.
     * The map contains field names as keys and the new values as values.
     * 
     * @param oldEntity The entity before the update
     * @param newEntity The entity after the update
     * @return Map of changed field names to their new values
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> getChangedFields(Object oldEntity, Object newEntity) {
        Map<String, Object> changedFields = new HashMap<>();
        
        try {
            // Convert both entities to Maps for comparison
            Map<String, Object> oldMap = objectMapper.convertValue(oldEntity, Map.class);
            Map<String, Object> newMap = objectMapper.convertValue(newEntity, Map.class);
            
            logger.debug("Comparing {} fields between old and new entity states", oldMap.size());
            
            // Compare all fields in the new entity
            for (Map.Entry<String, Object> newEntry : newMap.entrySet()) {
                String fieldName = newEntry.getKey();
                Object newValue = newEntry.getValue();
                Object oldValue = oldMap.get(fieldName);
                
                // Skip certain fields that are not meaningful for change tracking
                if (shouldSkipField(fieldName)) {
                    continue;
                }
                
                // Compare values (handling null cases and missing fields)
                // If field exists in new but not old, or values differ, it's a change
                boolean isChanged = false;
                if (!oldMap.containsKey(fieldName)) {
                    // Field exists in new but not in old (could be due to @JsonBackReference)
                    // Only consider it changed if newValue is not null
                    isChanged = (newValue != null);
                } else if (!areValuesEqual(oldValue, newValue)) {
                    isChanged = true;
                }
                
                if (isChanged) {
                    // For entity references (ManyToOne/OneToOne), capture just the ID
                    if (newValue instanceof Map) {
                        Map<String, Object> newValueMap = (Map<String, Object>) newValue;
                        
                        // Check if this looks like an entity reference (has 'id' field)
                        if (newValueMap.containsKey("id")) {
                            Object newId = newValueMap.get("id");
                            Object oldId = null;
                            
                            // Try to get old ID if oldValue is also a Map
                            if (oldValue instanceof Map) {
                                Map<String, Object> oldValueMap = (Map<String, Object>) oldValue;
                                oldId = oldValueMap.get("id");
                            }
                            
                            // Store a simplified representation: field name -> new ID
                            Map<String, Object> entityRef = new HashMap<>();
                            entityRef.put("id", newId);
                            // Include a readable name if available
                            if (newValueMap.containsKey("name")) {
                                entityRef.put("name", newValueMap.get("name"));
                            } else if (newValueMap.containsKey("roomNumber")) {
                                entityRef.put("roomNumber", newValueMap.get("roomNumber"));
                            } else if (newValueMap.containsKey("reservationNumber")) {
                                entityRef.put("reservationNumber", newValueMap.get("reservationNumber"));
                            } else if (newValueMap.containsKey("firstName") && newValueMap.containsKey("lastName")) {
                                entityRef.put("firstName", newValueMap.get("firstName"));
                                entityRef.put("lastName", newValueMap.get("lastName"));
                            }
                            changedFields.put(fieldName, entityRef);
                            logger.debug("Detected change in entity reference field '{}': {} -> {}", 
                                    fieldName, oldId, newId);
                        } else {
                            // It's a nested object but not an entity reference, include it as is
                            changedFields.put(fieldName, newValue);
                            logger.debug("Detected change in nested object field '{}'", fieldName);
                        }
                    } else {
                        // Regular field change
                        changedFields.put(fieldName, newValue);
                        logger.debug("Detected change in field '{}': {} -> {}", fieldName, oldValue, newValue);
                    }
                }
            }
            
            // Also check for fields that exist in old but not in new (field was removed/nullified)
            for (Map.Entry<String, Object> oldEntry : oldMap.entrySet()) {
                String fieldName = oldEntry.getKey();
                if (shouldSkipField(fieldName)) {
                    continue;
                }
                
                // If field exists in old but not in new, and old value is not null, it's a change
                if (!newMap.containsKey(fieldName) && oldEntry.getValue() != null) {
                    // Field was removed or set to null
                    changedFields.put(fieldName, null);
                    logger.debug("Detected removal/nullification of field '{}'", fieldName);
                }
            }
            
            logger.debug("Found {} changed field(s) out of {} total fields", changedFields.size(), newMap.size());
            
        } catch (Exception e) {
            logger.error("Error comparing entities for changed fields", e);
            // Fallback: return empty map
        }
        
        return changedFields;
    }
    
    /**
     * Checks if a field should be skipped during change detection.
     * Fields like 'class', 'createdAt', 'updatedAt' are typically not meaningful for change tracking.
     */
    private boolean shouldSkipField(String fieldName) {
        // Skip JPA/Hibernate internal fields and timestamps that change automatically
        return fieldName.equals("class") || 
               fieldName.equals("createdAt") || 
               fieldName.equals("updatedAt") ||
               fieldName.equals("hibernateLazyInitializer") ||
               fieldName.equals("reservations") || // Skip collections to avoid circular references
               fieldName.equals("rooms") ||
               fieldName.equals("invoices") ||
               fieldName.equals("roomTypeRates");
    }
    
    /**
     * Compares two values for equality, handling null cases and common types.
     */
    private boolean areValuesEqual(Object oldValue, Object newValue) {
        if (oldValue == null && newValue == null) {
            return true;
        }
        if (oldValue == null || newValue == null) {
            return false;
        }
        
        // Use equals for comparison
        return oldValue.equals(newValue);
    }
}


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
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logCreate(String entityType, Long entityId, Object entity) {
        logOperation(AuditLog.AuditAction.CREATE, entityType, entityId, null, entity);
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

            // Serialize entity states to JSON
            if (oldEntity != null) {
                logger.debug("Serializing old entity state for {} {}", entityType, entityId);
                try {
                    String oldValueJson = objectMapper.writeValueAsString(oldEntity);
                    auditLog.setOldValue(oldValueJson);
                    logger.debug("Successfully serialized old entity state (length: {} chars)", oldValueJson.length());
                } catch (Exception e) {
                    logger.warn("Failed to serialize old entity state for audit log, using toString()", e);
                    auditLog.setOldValue(oldEntity.toString());
                }
            } else {
                logger.debug("No old entity provided for {} {}", entityType, entityId);
            }

            if (newEntity != null) {
                logger.debug("Serializing new entity state for {} {}", entityType, entityId);
                try {
                    String newValueJson = objectMapper.writeValueAsString(newEntity);
                    auditLog.setNewValue(newValueJson);
                    logger.debug("Successfully serialized new entity state (length: {} chars)", newValueJson.length());
                } catch (Exception e) {
                    logger.warn("Failed to serialize new entity state for audit log, using toString()", e);
                    auditLog.setNewValue(newEntity.toString());
                }
            } else {
                logger.debug("No new entity provided for {} {}", entityType, entityId);
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
}


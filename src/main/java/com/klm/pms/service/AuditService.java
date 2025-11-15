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
        try {
            String username = SecurityContextUtil.getCurrentUsername();
            if (username == null) {
                username = "SYSTEM"; // Fallback for system operations
            }

            AuditLog auditLog = new AuditLog();
            auditLog.setEntityType(entityType);
            auditLog.setEntityId(entityId);
            auditLog.setAction(action);
            auditLog.setUsername(username);
            auditLog.setTimestamp(LocalDateTime.now());

            // Serialize entity states to JSON
            if (oldEntity != null) {
                try {
                    auditLog.setOldValue(objectMapper.writeValueAsString(oldEntity));
                } catch (Exception e) {
                    logger.warn("Failed to serialize old entity state for audit log", e);
                    auditLog.setOldValue(oldEntity.toString());
                }
            }

            if (newEntity != null) {
                try {
                    auditLog.setNewValue(objectMapper.writeValueAsString(newEntity));
                } catch (Exception e) {
                    logger.warn("Failed to serialize new entity state for audit log", e);
                    auditLog.setNewValue(newEntity.toString());
                }
            }

            // Get request information if available
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                auditLog.setRequestPath(request.getRequestURI());
                auditLog.setRequestMethod(request.getMethod());
            }

            // Create description
            auditLog.setDescription(String.format("%s %s with ID %d", action, entityType, entityId));

            auditLogRepository.save(auditLog);
            logger.debug("Audit log created: {} {} {} by {}", action, entityType, entityId, username);
        } catch (Exception e) {
            // Don't let audit logging failures break the main operation
            logger.error("Failed to create audit log for {} {} {}: {}", action, entityType, entityId, e.getMessage(), e);
        }
    }
}


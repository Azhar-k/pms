package com.klm.pms.config;

import com.klm.pms.service.AuditService;
import jakarta.persistence.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * JPA Entity Listener for automatic audit logging.
 * This listener is triggered by JPA lifecycle events (@PrePersist, @PreUpdate, @PreRemove).
 */
@Component
public class AuditEntityListener {

    private static final Logger logger = LoggerFactory.getLogger(AuditEntityListener.class);

    private static ApplicationContext applicationContext;

    @Autowired
    public void setApplicationContext(ApplicationContext applicationContext) {
        AuditEntityListener.applicationContext = applicationContext;
    }

    @PrePersist
    public void prePersist(Object entity) {
        try {
            AuditService auditService = getAuditService();
            if (auditService != null) {
                String entityType = entity.getClass().getSimpleName();
                Long entityId = getEntityId(entity);
                auditService.logCreate(entityType, entityId, entity);
            }
        } catch (Exception e) {
            // Don't let audit logging failures break the main operation
            logger.error("Failed to audit CREATE operation for entity: {}", entity.getClass().getSimpleName(), e);
        }
    }

    @PreUpdate
    public void preUpdate(Object entity) {
        try {
            AuditService auditService = getAuditService();
            if (auditService != null) {
                String entityType = entity.getClass().getSimpleName();
                Long entityId = getEntityId(entity);
                // For updates, we log the new state (old state would require additional tracking)
                auditService.logUpdate(entityType, entityId, null, entity);
            }
        } catch (Exception e) {
            // Don't let audit logging failures break the main operation
            logger.error("Failed to audit UPDATE operation for entity: {}", entity.getClass().getSimpleName(), e);
        }
    }

    @PreRemove
    public void preRemove(Object entity) {
        try {
            AuditService auditService = getAuditService();
            if (auditService != null) {
                String entityType = entity.getClass().getSimpleName();
                Long entityId = getEntityId(entity);
                auditService.logDelete(entityType, entityId, entity);
            }
        } catch (Exception e) {
            // Don't let audit logging failures break the main operation
            logger.error("Failed to audit DELETE operation for entity: {}", entity.getClass().getSimpleName(), e);
        }
    }

    /**
     * Get the AuditService bean from the application context.
     * This is needed because EntityListeners are not managed by Spring.
     */
    private AuditService getAuditService() {
        if (applicationContext == null) {
            return null;
        }
        try {
            return applicationContext.getBean(AuditService.class);
        } catch (Exception e) {
            logger.warn("Could not get AuditService from application context", e);
            return null;
        }
    }

    /**
     * Extract the entity ID using reflection.
     */
    private Long getEntityId(Object entity) {
        try {
            // Try to get ID using common getter methods
            java.lang.reflect.Method getIdMethod = entity.getClass().getMethod("getId");
            Object id = getIdMethod.invoke(entity);
            if (id instanceof Long) {
                return (Long) id;
            } else if (id instanceof Number) {
                return ((Number) id).longValue();
            }
        } catch (Exception e) {
            logger.debug("Could not extract ID from entity: {}", entity.getClass().getSimpleName());
        }
        return null;
    }
}


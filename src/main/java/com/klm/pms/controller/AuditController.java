package com.klm.pms.controller;

import com.klm.pms.dto.AuditLogDTO;
import com.klm.pms.dto.PageResponse;
import com.klm.pms.model.AuditLog;
import com.klm.pms.repository.AuditLogRepository;
import com.klm.pms.security.RequireRole;
import com.klm.pms.util.SecurityContextUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@CrossOrigin("*")
@RestController
@RequestMapping("/api/audit-logs")
@Tag(name = "Audit Logs", description = "APIs for retrieving audit logs (Admin only)")
@RequireRole("admin")
public class AuditController {

    private static final Logger logger = LoggerFactory.getLogger(AuditController.class);

    @Autowired
    private AuditLogRepository auditLogRepository;

    @GetMapping
    @Operation(summary = "Get all audit logs", description = "Retrieves audit logs with pagination and filtering. Admin only.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Audit logs retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Admin access required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required")
    })
    public ResponseEntity<PageResponse<AuditLogDTO>> getAllAuditLogs(
            @Parameter(description = "Page number (0-indexed)") @RequestParam(required = false, defaultValue = "0") Integer page,
            @Parameter(description = "Page size") @RequestParam(required = false, defaultValue = "20") Integer size,
            @Parameter(description = "Sort by field (e.g., timestamp, username)") @RequestParam(required = false, defaultValue = "timestamp") String sortBy,
            @Parameter(description = "Sort direction (asc or desc)") @RequestParam(required = false, defaultValue = "desc") String sortDir,
            @Parameter(description = "Entity type filter (e.g., Guest, Room)") @RequestParam(required = false) String entityType,
            @Parameter(description = "Entity ID filter") @RequestParam(required = false) Long entityId,
            @Parameter(description = "Username filter") @RequestParam(required = false) String username,
            @Parameter(description = "Action filter (CREATE, UPDATE, DELETE)") @RequestParam(required = false) AuditLog.AuditAction action,
            @Parameter(description = "Start date filter (ISO format)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "End date filter (ISO format)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        logger.info("GET /api/audit-logs - Fetching audit logs by admin: {}", SecurityContextUtil.getCurrentUsername());
        
        // Default sorting by timestamp descending
        Sort.Direction direction = sortDir != null && sortDir.equalsIgnoreCase("asc") 
                ? Sort.Direction.ASC 
                : Sort.Direction.DESC;
        Sort sort = Sort.by(direction, sortBy != null ? sortBy : "timestamp");
        
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<AuditLog> auditLogPage;
        
        // Use filtered query if any filters are provided
        if (entityType != null || entityId != null || username != null || action != null || startDate != null || endDate != null) {
            auditLogPage = auditLogRepository.findByFilters(
                    entityType, entityId, username, action, startDate, endDate, pageable);
        } else {
            auditLogPage = auditLogRepository.findAll(pageable);
        }
        
        List<AuditLogDTO> auditLogDTOs = auditLogPage.getContent().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        
        PageResponse<AuditLogDTO> response = new PageResponse<>(
                auditLogDTOs,
                auditLogPage.getNumber(),
                auditLogPage.getSize(),
                auditLogPage.getTotalElements()
        );
        
        logger.info("GET /api/audit-logs - Retrieved {} audit log(s) out of {} total", 
                auditLogDTOs.size(), auditLogPage.getTotalElements());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get audit log by ID", description = "Retrieves a specific audit log by its ID. Admin only.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Audit log found"),
            @ApiResponse(responseCode = "404", description = "Audit log not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Admin access required")
    })
    public ResponseEntity<AuditLogDTO> getAuditLogById(
            @Parameter(description = "Audit log ID", required = true) @PathVariable Long id) {
        
        logger.info("GET /api/audit-logs/{} - Fetching audit log by ID", id);
        
        AuditLog auditLog = auditLogRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("Audit log not found with ID: {}", id);
                    return new RuntimeException("Audit log not found with id: " + id);
                });
        
        return ResponseEntity.ok(toDTO(auditLog));
    }

    @GetMapping("/entity/{entityType}/{entityId}")
    @Operation(summary = "Get audit logs for a specific entity", description = "Retrieves all audit logs for a specific entity. Admin only.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Audit logs retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Admin access required")
    })
    public ResponseEntity<List<AuditLogDTO>> getAuditLogsByEntity(
            @Parameter(description = "Entity type (e.g., Guest, Room)", required = true) @PathVariable String entityType,
            @Parameter(description = "Entity ID", required = true) @PathVariable Long entityId) {
        
        logger.info("GET /api/audit-logs/entity/{}/{} - Fetching audit logs for entity", entityType, entityId);
        
        List<AuditLog> auditLogs = auditLogRepository.findByEntityTypeAndEntityIdOrderByTimestampDesc(entityType, entityId);
        
        List<AuditLogDTO> auditLogDTOs = auditLogs.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        
        logger.info("GET /api/audit-logs/entity/{}/{} - Retrieved {} audit log(s)", entityType, entityId, auditLogDTOs.size());
        
        return ResponseEntity.ok(auditLogDTOs);
    }

    /**
     * Convert AuditLog entity to DTO.
     */
    private AuditLogDTO toDTO(AuditLog auditLog) {
        AuditLogDTO dto = new AuditLogDTO();
        dto.setId(auditLog.getId());
        dto.setEntityType(auditLog.getEntityType());
        dto.setEntityId(auditLog.getEntityId());
        dto.setAction(auditLog.getAction());
        dto.setUsername(auditLog.getUsername());
        dto.setTimestamp(auditLog.getTimestamp());
        dto.setChanges(auditLog.getChanges());
        dto.setDescription(auditLog.getDescription());
        dto.setRequestPath(auditLog.getRequestPath());
        dto.setRequestMethod(auditLog.getRequestMethod());
        return dto;
    }
}


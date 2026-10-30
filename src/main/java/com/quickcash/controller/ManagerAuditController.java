package com.quickcash.controller;

import com.quickcash.dto.AuditLogResponse;
import com.quickcash.service.AuditService;

import org.springframework.http.ResponseEntity;

import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/manager/audit-logs")
@PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
public class ManagerAuditController {

    private final AuditService auditService;

    public ManagerAuditController(
            AuditService auditService
    ) {
        this.auditService =
                auditService;
    }

    @GetMapping
    public ResponseEntity<List<AuditLogResponse>>
    getAllAuditLogs() {

        return ResponseEntity.ok(
                auditService.getAll()
        );
    }

    @GetMapping("/entity/{entityType}/{entityId}")
    public ResponseEntity<List<AuditLogResponse>>
    getEntityAuditLogs(
            @PathVariable String entityType,
            @PathVariable Long entityId
    ) {

        return ResponseEntity.ok(
                auditService.getForEntity(
                        entityType,
                        entityId
                )
        );
    }
}
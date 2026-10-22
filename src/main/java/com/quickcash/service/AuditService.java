package com.quickcash.service;

import com.quickcash.dto.AuditLogResponse;

import com.quickcash.entity.AuditAction;
import com.quickcash.entity.AuditLog;
import com.quickcash.entity.User;

import com.quickcash.repository.AuditLogRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(
            AuditLogRepository auditLogRepository
    ) {
        this.auditLogRepository =
                auditLogRepository;
    }

    @Transactional
    public void record(
            User actor,
            AuditAction action,
            String entityType,
            Long entityId,
            String referenceCode,
            String description
    ) {

        AuditLog log =
                new AuditLog();

        if (actor != null) {

            log.setActorUserId(
                    actor.getUserId()
            );

            log.setActorEmail(
                    actor.getEmail()
            );

            if (actor.getRole() != null) {
                log.setActorRole(
                        actor.getRole().name()
                );
            }
        }

        log.setAction(action);

        log.setEntityType(
                entityType
        );

        log.setEntityId(
                entityId
        );

        log.setReferenceCode(
                referenceCode
        );

        log.setDescription(
                description
        );

        auditLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> getAll() {

        return auditLogRepository
                .findAllByOrderByCreatedAtDesc()
                .stream()
                .map(AuditLogResponse::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> getForEntity(
            String entityType,
            Long entityId
    ) {

        return auditLogRepository
                .findByEntityTypeAndEntityIdOrderByCreatedAtDesc(
                        entityType,
                        entityId
                )
                .stream()
                .map(AuditLogResponse::new)
                .toList();
    }
}
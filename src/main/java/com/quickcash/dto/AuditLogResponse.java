package com.quickcash.dto;

import com.quickcash.entity.AuditLog;

import java.time.LocalDateTime;

public class AuditLogResponse {

    private Long auditId;

    private Long actorUserId;
    private String actorEmail;
    private String actorRole;

    private String action;

    private String entityType;
    private Long entityId;

    private String referenceCode;
    private String description;

    private LocalDateTime createdAt;

    public AuditLogResponse(AuditLog log) {

        this.auditId =
                log.getAuditId();

        this.actorUserId =
                log.getActorUserId();

        this.actorEmail =
                log.getActorEmail();

        this.actorRole =
                log.getActorRole();

        this.action =
                log.getAction().name();

        this.entityType =
                log.getEntityType();

        this.entityId =
                log.getEntityId();

        this.referenceCode =
                log.getReferenceCode();

        this.description =
                log.getDescription();

        this.createdAt =
                log.getCreatedAt();
    }

    public Long getAuditId() {
        return auditId;
    }

    public Long getActorUserId() {
        return actorUserId;
    }

    public String getActorEmail() {
        return actorEmail;
    }

    public String getActorRole() {
        return actorRole;
    }

    public String getAction() {
        return action;
    }

    public String getEntityType() {
        return entityType;
    }

    public Long getEntityId() {
        return entityId;
    }

    public String getReferenceCode() {
        return referenceCode;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
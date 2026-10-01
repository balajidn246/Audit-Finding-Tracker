package com.internship.tool.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_logs_finding_created", columnList = "finding_id, created_at"),
        @Index(name = "idx_audit_logs_actor_created", columnList = "actor_id, created_at")
})
public class AuditLogEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true, updatable = false)
    private UUID eventId;

    @Column(name = "finding_id", updatable = false)
    private Long findingId;

    @Column(name = "actor_id", updatable = false)
    private Long actorId;

    @Column(name = "entity_type", nullable = false, length = 50, updatable = false)
    private String entityType = "FINDING";

    @Column(nullable = false, length = 100, updatable = false)
    private String action;

    @Column(name = "old_json", columnDefinition = "text", updatable = false)
    private String oldJson;

    @Column(name = "new_json", columnDefinition = "text", updatable = false)
    private String newJson;

    @Column(name = "source_ip", length = 64, updatable = false)
    private String sourceIp;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void assignEventId() { if (eventId == null) eventId = UUID.randomUUID(); }

    public Long getId() { return id; }
    public UUID getEventId() { return eventId; }
    public Long getFindingId() { return findingId; }
    public void setFindingId(Long findingId) { this.findingId = findingId; }
    public Long getActorId() { return actorId; }
    public void setActorId(Long actorId) { this.actorId = actorId; }
    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getOldJson() { return oldJson; }
    public void setOldJson(String oldJson) { this.oldJson = oldJson; }
    public String getNewJson() { return newJson; }
    public void setNewJson(String newJson) { this.newJson = newJson; }
    public String getSourceIp() { return sourceIp; }
    public void setSourceIp(String sourceIp) { this.sourceIp = sourceIp; }
    public Instant getCreatedAt() { return createdAt; }
}

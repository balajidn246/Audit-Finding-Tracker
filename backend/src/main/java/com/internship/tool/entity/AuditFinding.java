package com.internship.tool.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "audit_findings", indexes = {
        @Index(name = "idx_findings_status_updated", columnList = "status, updated_at"),
        @Index(name = "idx_findings_owner_status", columnList = "owner_id, status"),
        @Index(name = "idx_findings_due_date", columnList = "due_date")
})
public class AuditFinding {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false)
    private UUID uuid;

    @Column(nullable = false, length = 1000)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Severity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private FindingStatus status;

    @Column(name = "root_cause", columnDefinition = "text")
    private String rootCause;

    @Column(columnDefinition = "text")
    private String recommendation;

    @Column(name = "management_response", columnDefinition = "text")
    private String managementResponse;

    @Column(name = "source_audit", length = 200)
    private String sourceAudit;

    @Column(name = "business_unit", length = 160)
    private String businessUnit;

    @Column(name = "owner_id")
    private Long ownerId;

    @Column(name = "created_by", updatable = false)
    private Long createdById;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "risk_acceptance_expires_on")
    private LocalDate riskAcceptanceExpiresOn;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(nullable = false)
    private boolean deleted;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void assignUuid() {
        if (uuid == null) uuid = UUID.randomUUID();
        if (status == null) status = FindingStatus.OPEN;
    }

    public Long getId() { return id; }
    public UUID getUuid() { return uuid; }
    public void setUuid(UUID uuid) { this.uuid = uuid; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Severity getSeverity() { return severity; }
    public void setSeverity(Severity severity) { this.severity = severity; }
    public FindingStatus getStatus() { return status; }
    public void setStatus(FindingStatus status) { this.status = status; }
    public String getRootCause() { return rootCause; }
    public void setRootCause(String rootCause) { this.rootCause = rootCause; }
    public String getRecommendation() { return recommendation; }
    public void setRecommendation(String recommendation) { this.recommendation = recommendation; }
    public String getManagementResponse() { return managementResponse; }
    public void setManagementResponse(String managementResponse) { this.managementResponse = managementResponse; }
    public String getSourceAudit() { return sourceAudit; }
    public void setSourceAudit(String sourceAudit) { this.sourceAudit = sourceAudit; }
    public String getBusinessUnit() { return businessUnit; }
    public void setBusinessUnit(String businessUnit) { this.businessUnit = businessUnit; }
    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }
    public Long getCreatedById() { return createdById; }
    public void setCreatedById(Long createdById) { this.createdById = createdById; }
    public java.time.LocalDate getDueDate() { return dueDate; }
    public void setDueDate(java.time.LocalDate dueDate) { this.dueDate = dueDate; }
    public Instant getClosedAt() { return closedAt; }
    public void setClosedAt(Instant closedAt) { this.closedAt = closedAt; }
    public LocalDate getRiskAcceptanceExpiresOn() { return riskAcceptanceExpiresOn; }
    public void setRiskAcceptanceExpiresOn(LocalDate expiresOn) { this.riskAcceptanceExpiresOn = expiresOn; }
    public long getVersion() { return version; }
    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public enum Severity { LOW, MEDIUM, HIGH, CRITICAL }
    public enum FindingStatus { OPEN, IN_PROGRESS, PENDING_VALIDATION, REWORK_REQUIRED, CLOSED, RISK_ACCEPTED }
}

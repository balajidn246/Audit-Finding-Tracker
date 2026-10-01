package com.internship.tool.dto;

import com.internship.tool.entity.AuditFinding;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public final class FindingDtos {
    private FindingDtos() { }

    public record CreateRequest(
            @NotBlank @Size(max = 1000) String title,
            @NotBlank @Size(max = 12000) String description,
            @NotNull AuditFinding.Severity severity,
            @Size(max = 12000) String rootCause,
            @Size(max = 12000) String recommendation,
            @Size(max = 12000) String managementResponse,
            @Size(max = 200) String sourceAudit,
            @Size(max = 160) String businessUnit,
            Long ownerId,
            LocalDate dueDate) { }

    public record UpdateRequest(
            @Size(max = 1000) String title,
            @Size(max = 12000) String description,
            AuditFinding.Severity severity,
            @Size(max = 12000) String rootCause,
            @Size(max = 12000) String recommendation,
            @Size(max = 12000) String managementResponse,
            @Size(max = 200) String sourceAudit,
            @Size(max = 160) String businessUnit,
            Long ownerId,
            LocalDate dueDate) { }

    public record StatusRequest(@NotNull AuditFinding.FindingStatus status,
                                @Size(max = 2000) String reason,
                                java.time.LocalDate riskAcceptanceExpiresOn) { }

    public record FindingView(UUID uuid, String title, String description,
                              AuditFinding.Severity severity, AuditFinding.FindingStatus status,
                              String rootCause, String recommendation, String managementResponse,
                              String sourceAudit, String businessUnit, Long ownerId, Long createdById,
                              LocalDate dueDate, Instant closedAt, LocalDate riskAcceptanceExpiresOn, long version,
                              Instant createdAt, Instant updatedAt) {
        public static FindingView from(AuditFinding f) {
            return new FindingView(f.getUuid(), f.getTitle(), f.getDescription(), f.getSeverity(), f.getStatus(),
                    f.getRootCause(), f.getRecommendation(), f.getManagementResponse(), f.getSourceAudit(),
                    f.getBusinessUnit(), f.getOwnerId(), f.getCreatedById(), f.getDueDate(), f.getClosedAt(), f.getRiskAcceptanceExpiresOn(),
                    f.getVersion(), f.getCreatedAt(), f.getUpdatedAt());
        }
    }

    public record AuditEventView(UUID eventId, Long actorId, String entityType, String action,
                                 String oldJson, String newJson, String sourceIp, Instant createdAt) { }
}

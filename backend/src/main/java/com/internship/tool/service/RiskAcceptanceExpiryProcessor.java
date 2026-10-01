package com.internship.tool.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.internship.tool.entity.AuditFinding;
import com.internship.tool.entity.AuditFinding.FindingStatus;
import com.internship.tool.entity.AuditLogEntry;
import com.internship.tool.repository.AuditFindingRepository;
import com.internship.tool.repository.AuditLogRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.Map;

@Service
public class RiskAcceptanceExpiryProcessor {
    private static final int BATCH_SIZE = 500;
    private final AuditFindingRepository findings;
    private final AuditLogRepository audit;
    private final ObjectMapper mapper;
    private final FindingNotificationService notifications;

    public RiskAcceptanceExpiryProcessor(AuditFindingRepository findings, AuditLogRepository audit,
                                         ObjectMapper mapper, FindingNotificationService notifications) {
        this.findings = findings;
        this.audit = audit;
        this.mapper = mapper;
        this.notifications = notifications;
    }

    @Transactional
    public int reopenBatch(LocalDate today) {
        var expired = findings.findByStatusAndRiskAcceptanceExpiresOnLessThanEqualAndDeletedFalse(
                FindingStatus.RISK_ACCEPTED, today, PageRequest.of(0, BATCH_SIZE));
        for (AuditFinding finding : expired.getContent()) {
            Map<String, Object> before = Map.of("uuid", finding.getUuid(), "status", finding.getStatus().name(),
                    "riskAcceptanceExpiresOn", finding.getRiskAcceptanceExpiresOn().toString());
            finding.setStatus(FindingStatus.OPEN);
            finding.setRiskAcceptanceExpiresOn(null);
            finding.setClosedAt(null);
            AuditFinding saved = findings.save(finding);
            AuditLogEntry event = new AuditLogEntry();
            event.setFindingId(saved.getId());
            event.setEntityType("FINDING");
            event.setAction("RISK_ACCEPTANCE_EXPIRED");
            try {
                event.setOldJson(mapper.writeValueAsString(before));
                event.setNewJson(mapper.writeValueAsString(Map.of("uuid", saved.getUuid(), "status", "OPEN", "actor", "SYSTEM")));
            } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
                throw new IllegalStateException("Could not write expiry audit event", ex);
            }
            audit.save(event);
            notifications.queueOwnerMessage(saved, "EXPIRED");
        }
        return expired.getNumberOfElements();
    }
}

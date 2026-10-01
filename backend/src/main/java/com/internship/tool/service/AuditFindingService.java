package com.internship.tool.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.internship.tool.dto.FindingDtos;
import com.internship.tool.entity.AuditFinding;
import com.internship.tool.entity.AuditFinding.FindingStatus;
import com.internship.tool.entity.AuditLogEntry;
import com.internship.tool.entity.User;
import com.internship.tool.repository.AuditFindingRepository;
import com.internship.tool.repository.AuditLogRepository;
import com.internship.tool.repository.EvidenceFileRepository;
import com.internship.tool.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import java.nio.charset.StandardCharsets;

@Service
public class AuditFindingService {
    private static final Set<String> SORT_FIELDS = Set.of("createdAt", "updatedAt", "dueDate", "severity", "status", "title");
    private final AuditFindingRepository findings;
    private final AuditLogRepository auditLogs;
    private final UserRepository users;
    private final EvidenceFileRepository evidence;
    private final FindingNotificationService notifications;
    private final ObjectMapper objectMapper;

    public AuditFindingService(AuditFindingRepository findings, AuditLogRepository auditLogs,
                               UserRepository users, EvidenceFileRepository evidence,
                               FindingNotificationService notifications, ObjectMapper objectMapper) {
        this.findings = findings;
        this.auditLogs = auditLogs;
        this.users = users;
        this.evidence = evidence;
        this.notifications = notifications;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public FindingDtos.FindingView create(FindingDtos.CreateRequest request, Authentication auth, String sourceIp) {
        User actor = actor(auth);
        requirePrivileged(auth);
        validateOwner(request.ownerId());
        AuditFinding finding = new AuditFinding();
        finding.setTitle(request.title().trim());
        finding.setDescription(request.description().trim());
        finding.setSeverity(request.severity());
        finding.setStatus(FindingStatus.OPEN);
        finding.setRootCause(trimToNull(request.rootCause()));
        finding.setRecommendation(trimToNull(request.recommendation()));
        finding.setManagementResponse(trimToNull(request.managementResponse()));
        finding.setSourceAudit(trimToNull(request.sourceAudit()));
        finding.setBusinessUnit(trimToNull(request.businessUnit()));
        finding.setOwnerId(request.ownerId());
        finding.setCreatedById(actor.getId());
        finding.setDueDate(request.dueDate());
        AuditFinding saved = findings.save(finding);
        log(saved, actor, "FINDING_CREATED", null, snapshot(saved), sourceIp);
        notifications.queueOwnerMessage(saved, "CREATED");
        return FindingDtos.FindingView.from(saved);
    }

    @Transactional(readOnly = true)
    public Page<FindingDtos.FindingView> search(String query, FindingStatus status,
                                                AuditFinding.Severity severity, Long ownerId,
                                                int page, int size, String sortBy, String direction) {
        if (page < 0) throw new IllegalArgumentException("page must be zero or greater");
        if (size < 1 || size > 100) throw new IllegalArgumentException("size must be between 1 and 100");
        if (query != null && query.length() > 200) throw new IllegalArgumentException("q must be 200 characters or fewer");
        if (!SORT_FIELDS.contains(sortBy)) throw new IllegalArgumentException("Unsupported sort field");
        Sort.Direction sortDirection;
        try { sortDirection = Sort.Direction.fromString(direction); }
        catch (RuntimeException ex) { throw new IllegalArgumentException("dir must be ASC or DESC"); }
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        String normalizedQuery = query == null ? null : query.trim().toLowerCase(Locale.ROOT);
        return findings.findAll((root, criteria, builder) -> {
            Predicate active = builder.isFalse(root.get("deleted"));
            if (status != null) active = builder.and(active, builder.equal(root.get("status"), status));
            if (severity != null) active = builder.and(active, builder.equal(root.get("severity"), severity));
            if (ownerId != null) active = builder.and(active, builder.equal(root.get("ownerId"), ownerId));
            if (normalizedQuery != null && !normalizedQuery.isBlank()) {
                String pattern = "%" + normalizedQuery.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
                active = builder.and(active, builder.or(
                        builder.like(builder.lower(root.get("title")), pattern, '\\'),
                        builder.like(builder.lower(root.get("description")), pattern, '\\'),
                        builder.like(builder.lower(root.get("sourceAudit")), pattern, '\\'),
                        builder.like(builder.lower(root.get("businessUnit")), pattern, '\\')));
            }
            return active;
        }, pageable).map(FindingDtos.FindingView::from);
    }

    @Transactional(readOnly = true)
    public FindingDtos.FindingView get(UUID uuid) {
        return FindingDtos.FindingView.from(findActive(uuid));
    }

    @Transactional
    public FindingDtos.FindingView update(UUID uuid, FindingDtos.UpdateRequest request,
                                          Authentication auth, String sourceIp) {
        AuditFinding finding = findActive(uuid);
        User actor = actor(auth);
        boolean privileged = isPrivileged(auth);
        if (!privileged && !actor.getId().equals(finding.getOwnerId())) throw new AccessDeniedException("Only the owner or an audit manager can update this finding");
        if (!privileged && hasOwnerForbiddenChanges(request)) throw new AccessDeniedException("Finding details can only be changed by an audit manager");
        Map<String, Object> before = snapshot(finding);
        Long previousOwnerId = finding.getOwnerId();
        if (request.title() != null) finding.setTitle(request.title().trim());
        if (request.description() != null) finding.setDescription(request.description().trim());
        if (request.severity() != null) finding.setSeverity(request.severity());
        if (request.rootCause() != null) finding.setRootCause(trimToNull(request.rootCause()));
        if (request.recommendation() != null) finding.setRecommendation(trimToNull(request.recommendation()));
        if (request.managementResponse() != null) finding.setManagementResponse(trimToNull(request.managementResponse()));
        if (request.sourceAudit() != null) finding.setSourceAudit(trimToNull(request.sourceAudit()));
        if (request.businessUnit() != null) finding.setBusinessUnit(trimToNull(request.businessUnit()));
        if (request.ownerId() != null) { validateOwner(request.ownerId()); finding.setOwnerId(request.ownerId()); }
        if (request.dueDate() != null) finding.setDueDate(request.dueDate());
        AuditFinding saved = findings.save(finding);
        log(saved, actor, "FINDING_UPDATED", before, snapshot(saved), sourceIp);
        if (!java.util.Objects.equals(previousOwnerId, saved.getOwnerId())) notifications.queueOwnerMessage(saved, "ASSIGNED");
        return FindingDtos.FindingView.from(saved);
    }

    @Transactional
    public FindingDtos.FindingView transition(UUID uuid, FindingDtos.StatusRequest request,
                                              Authentication auth, String sourceIp) {
        AuditFinding finding = findActive(uuid);
        User actor = actor(auth);
        FindingStatus from = finding.getStatus();
        FindingStatus to = request.status();
        if (from == to) return FindingDtos.FindingView.from(finding);
        boolean privileged = isPrivileged(auth);
        boolean owner = actor.getId().equals(finding.getOwnerId());
        if (!allowedTransition(from, to, privileged, owner)) {
            throw new IllegalArgumentException("Status transition is not permitted");
        }
        if (to == FindingStatus.RISK_ACCEPTED && (request.reason() == null || request.reason().isBlank())) {
            throw new IllegalArgumentException("A reason is required to accept risk");
        }
        if (to == FindingStatus.RISK_ACCEPTED && (request.riskAcceptanceExpiresOn() == null
                || !request.riskAcceptanceExpiresOn().isAfter(java.time.LocalDate.now(java.time.ZoneOffset.UTC)))) {
            throw new IllegalArgumentException("A future risk-acceptance expiry date is required");
        }
        if (to == FindingStatus.CLOSED && finding.getOwnerId() != null && actor.getId().equals(finding.getOwnerId())) {
            throw new AccessDeniedException("The finding owner cannot approve their own closure");
        }
        if (to == FindingStatus.CLOSED && evidence.countByFindingId(finding.getId()) == 0) {
            throw new IllegalArgumentException("At least one malware-scanned evidence file is required before closure");
        }
        Map<String, Object> before = snapshot(finding);
        finding.setStatus(to);
        finding.setClosedAt(to == FindingStatus.CLOSED ? Instant.now() : null);
        finding.setRiskAcceptanceExpiresOn(to == FindingStatus.RISK_ACCEPTED ? request.riskAcceptanceExpiresOn() : null);
        AuditFinding saved = findings.save(finding);
        String eventAction = "STATUS_CHANGED";
        Map<String, Object> after = snapshot(saved);
        after.put("reason", trimToNull(request.reason()));
        log(saved, actor, eventAction, before, after, sourceIp);
        if (!java.util.Objects.equals(saved.getOwnerId(), actor.getId())) notifications.queueOwnerMessage(saved, "STATUS_CHANGED");
        return FindingDtos.FindingView.from(saved);
    }

    @Transactional
    public void softDelete(UUID uuid, Authentication auth, String sourceIp) {
        requirePrivileged(auth);
        AuditFinding finding = findActive(uuid);
        User actor = actor(auth);
        Map<String, Object> before = snapshot(finding);
        finding.setDeleted(true);
        AuditFinding saved = findings.save(finding);
        log(saved, actor, "FINDING_ARCHIVED", before, snapshot(saved), sourceIp);
    }

    @Transactional(readOnly = true)
    public Page<FindingDtos.AuditEventView> history(UUID uuid, int page, int size) {
        if (page < 0 || size < 1 || size > 100) throw new IllegalArgumentException("Invalid page or size");
        AuditFinding finding = findActive(uuid);
        return auditLogs.findByFindingIdOrderByCreatedAtDesc(finding.getId(), PageRequest.of(page, size))
                .map(event -> new FindingDtos.AuditEventView(event.getEventId(), event.getActorId(),
                        event.getEntityType(), event.getAction(), event.getOldJson(), event.getNewJson(),
                        event.getSourceIp(), event.getCreatedAt()));
    }

    @Transactional(readOnly = true)
    public Map<String, Long> stats() {
        Map<String, Long> result = new LinkedHashMap<>();
        result.put("total", findings.countByDeletedFalse());
        for (FindingStatus status : FindingStatus.values()) {
            result.put(status.name().toLowerCase(), findings.countByStatusAndDeletedFalse(status));
        }
        for (AuditFinding.Severity severity : AuditFinding.Severity.values()) {
            result.put("severity_" + severity.name().toLowerCase(), findings.countBySeverityAndDeletedFalse(severity));
        }
        return result;
    }

    @Transactional
    public byte[] exportCsv(String query, FindingStatus status, AuditFinding.Severity severity, Long ownerId,
                            Authentication auth, String sourceIp) {
        requirePrivileged(auth);
        StringBuilder csv = new StringBuilder("uuid,title,description,severity,status,owner_id,due_date,source_audit,business_unit,recommendation,management_response,created_at,updated_at\r\n");
        int pageNumber = 0; long exported = 0;
        Page<FindingDtos.FindingView> page;
        do {
            page = search(query, status, severity, ownerId, pageNumber, 100, "updatedAt", "DESC");
            for (FindingDtos.FindingView f : page.getContent()) {
                csv.append(cell(f.uuid())).append(',').append(cell(f.title())).append(',').append(cell(f.description())).append(',')
                        .append(cell(f.severity())).append(',').append(cell(f.status())).append(',').append(cell(f.ownerId())).append(',')
                        .append(cell(f.dueDate())).append(',').append(cell(f.sourceAudit())).append(',').append(cell(f.businessUnit())).append(',')
                        .append(cell(f.recommendation())).append(',').append(cell(f.managementResponse())).append(',')
                        .append(cell(f.createdAt())).append(',').append(cell(f.updatedAt())).append("\r\n");
                exported++;
                if (exported > 5000 || csv.length() > 25 * 1024 * 1024) throw new IllegalArgumentException("Export is too large; narrow the filters and retry");
            }
            pageNumber++;
        } while (page.hasNext());
        User actor = actor(auth);
        AuditLogEntry event = new AuditLogEntry(); event.setActorId(actor.getId()); event.setEntityType("REPORT");
        event.setAction("FINDING_REGISTER_EXPORTED"); event.setSourceIp(sourceIp);
        event.setNewJson(json(Map.of("rows", exported, "status", status == null ? "ALL" : status.name(),
                "severity", severity == null ? "ALL" : severity.name(), "ownerId", ownerId == null ? "ALL" : ownerId)));
        auditLogs.save(event);
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private AuditFinding findActive(UUID uuid) {
        return findings.findByUuidAndDeletedFalse(uuid).orElseThrow(() -> new NoSuchElementException("Finding not found"));
    }

    private User actor(Authentication auth) {
        return users.findByUsernameIgnoreCase(auth.getName()).orElseThrow(() -> new AccessDeniedException("User account is unavailable"));
    }

    private void validateOwner(Long ownerId) {
        if (ownerId != null && users.findById(ownerId).filter(User::isEnabled).isEmpty()) {
            throw new IllegalArgumentException("Owner must be an active user");
        }
    }

    private void requirePrivileged(Authentication auth) {
        if (!isPrivileged(auth)) throw new AccessDeniedException("An administrator or audit manager is required");
    }

    private boolean isPrivileged(Authentication auth) {
        return auth.getAuthorities().stream().map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_ADMIN") || role.equals("ROLE_MANAGER"));
    }

    private boolean hasOwnerForbiddenChanges(FindingDtos.UpdateRequest request) {
        return request.title() != null || request.description() != null || request.severity() != null
                || request.rootCause() != null || request.recommendation() != null || request.sourceAudit() != null
                || request.businessUnit() != null || request.ownerId() != null || request.dueDate() != null;
    }

    private boolean allowedTransition(FindingStatus from, FindingStatus to, boolean privileged, boolean owner) {
        if (privileged) {
            return switch (from) {
                case OPEN -> to == FindingStatus.IN_PROGRESS || to == FindingStatus.RISK_ACCEPTED;
                case IN_PROGRESS -> to == FindingStatus.PENDING_VALIDATION || to == FindingStatus.RISK_ACCEPTED;
                case PENDING_VALIDATION -> to == FindingStatus.CLOSED || to == FindingStatus.REWORK_REQUIRED || to == FindingStatus.RISK_ACCEPTED;
                case REWORK_REQUIRED -> to == FindingStatus.IN_PROGRESS || to == FindingStatus.RISK_ACCEPTED;
                case CLOSED, RISK_ACCEPTED -> to == FindingStatus.OPEN;
            };
        }
        if (!owner) return false;
        return (from == FindingStatus.OPEN && to == FindingStatus.IN_PROGRESS)
                || ((from == FindingStatus.IN_PROGRESS || from == FindingStatus.REWORK_REQUIRED)
                    && to == FindingStatus.PENDING_VALIDATION);
    }

    private Map<String, Object> snapshot(AuditFinding f) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("uuid", f.getUuid()); values.put("title", f.getTitle());
        values.put("description", f.getDescription()); values.put("severity", f.getSeverity());
        values.put("status", f.getStatus()); values.put("rootCause", f.getRootCause());
        values.put("recommendation", f.getRecommendation()); values.put("managementResponse", f.getManagementResponse());
        values.put("sourceAudit", f.getSourceAudit()); values.put("businessUnit", f.getBusinessUnit());
        values.put("ownerId", f.getOwnerId()); values.put("dueDate", f.getDueDate());
        values.put("closedAt", f.getClosedAt()); values.put("version", f.getVersion());
        values.put("riskAcceptanceExpiresOn", f.getRiskAcceptanceExpiresOn());
        return values;
    }

    private void log(AuditFinding finding, User actor, String action, Map<String, Object> before,
                     Map<String, Object> after, String sourceIp) {
        AuditLogEntry event = new AuditLogEntry();
        event.setFindingId(finding.getId()); event.setActorId(actor.getId());
        event.setAction(action); event.setSourceIp(sourceIp);
        event.setOldJson(json(before)); event.setNewJson(json(after));
        auditLogs.save(event);
    }

    private String json(Object value) {
        if (value == null) return null;
        try { return objectMapper.writeValueAsString(value); }
        catch (JsonProcessingException ex) { throw new IllegalStateException("Could not serialize audit event", ex); }
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String cell(Object raw) {
        if (raw == null) return "";
        String value = raw.toString().replace("\r", " ").replace("\n", " ");
        String check = value.stripLeading();
        if (!check.isEmpty() && "=+-@".indexOf(check.charAt(0)) >= 0) value = "'" + value;
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}

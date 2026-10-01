package com.internship.tool.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.internship.tool.dto.FindingDtos;
import com.internship.tool.entity.AuditFinding;
import com.internship.tool.entity.AuditLogEntry;
import com.internship.tool.entity.User;
import com.internship.tool.repository.AuditFindingRepository;
import com.internship.tool.repository.AuditLogRepository;
import com.internship.tool.repository.EvidenceFileRepository;
import com.internship.tool.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditFindingServiceWorkflowTest {
    @Mock private AuditFindingRepository findings;
    @Mock private AuditLogRepository auditLogs;
    @Mock private UserRepository users;
    @Mock private EvidenceFileRepository evidence;
    @Mock private FindingNotificationService notifications;

    private AuditFindingService service;

    @BeforeEach
    void setUp() {
        service = new AuditFindingService(findings, auditLogs, users, evidence, notifications,
                new ObjectMapper().registerModule(new JavaTimeModule()));
        lenient().when(findings.save(any(AuditFinding.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void assignedOwnerCanMoveOpenFindingIntoRemediationAndRequestValidation() {
        User owner = user(7L, "owner");
        AuditFinding finding = finding(AuditFinding.FindingStatus.OPEN, 7L);
        when(findings.findByUuidAndDeletedFalse(finding.getUuid())).thenReturn(Optional.of(finding));
        when(users.findByUsernameIgnoreCase("owner")).thenReturn(Optional.of(owner));

        var inProgress = service.transition(finding.getUuid(), status(AuditFinding.FindingStatus.IN_PROGRESS),
                auth("owner", "ROLE_VIEWER"), "192.0.2.10");
        assertEquals(AuditFinding.FindingStatus.IN_PROGRESS, inProgress.status());

        var pending = service.transition(finding.getUuid(), status(AuditFinding.FindingStatus.PENDING_VALIDATION),
                auth("owner", "ROLE_VIEWER"), "192.0.2.10");
        assertEquals(AuditFinding.FindingStatus.PENDING_VALIDATION, pending.status());
        verify(auditLogs, times(2)).save(any(AuditLogEntry.class));
        verify(notifications, never()).queueOwnerMessage(any(), anyString());
    }

    @Test
    void assignedManagerCannotApproveTheirOwnFinding() {
        User owner = user(7L, "reviewer");
        AuditFinding finding = finding(AuditFinding.FindingStatus.PENDING_VALIDATION, 7L);
        when(findings.findByUuidAndDeletedFalse(finding.getUuid())).thenReturn(Optional.of(finding));
        when(users.findByUsernameIgnoreCase("reviewer")).thenReturn(Optional.of(owner));

        assertThrows(AccessDeniedException.class, () -> service.transition(finding.getUuid(),
                status(AuditFinding.FindingStatus.CLOSED), auth("reviewer", "ROLE_MANAGER"), "192.0.2.11"));
        verify(findings, never()).save(any(AuditFinding.class));
        verify(auditLogs, never()).save(any(AuditLogEntry.class));
    }

    @Test
    void closureRequiresScannedEvidenceAndRecordsIndependentReviewer() {
        User reviewer = user(9L, "reviewer");
        AuditFinding finding = finding(AuditFinding.FindingStatus.PENDING_VALIDATION, 7L);
        when(findings.findByUuidAndDeletedFalse(finding.getUuid())).thenReturn(Optional.of(finding));
        when(users.findByUsernameIgnoreCase("reviewer")).thenReturn(Optional.of(reviewer));
        when(evidence.countByFindingId(finding.getId())).thenReturn(0L, 1L);
        var auth = auth("reviewer", "ROLE_MANAGER");

        assertThrows(IllegalArgumentException.class, () -> service.transition(finding.getUuid(),
                status(AuditFinding.FindingStatus.CLOSED), auth, "192.0.2.11"));
        assertEquals(AuditFinding.FindingStatus.PENDING_VALIDATION, finding.getStatus());

        var closed = service.transition(finding.getUuid(), status(AuditFinding.FindingStatus.CLOSED), auth, "192.0.2.11");
        assertEquals(AuditFinding.FindingStatus.CLOSED, closed.status());
        ArgumentCaptor<AuditLogEntry> event = ArgumentCaptor.forClass(AuditLogEntry.class);
        verify(auditLogs).save(event.capture());
        assertEquals("reviewer", reviewer.getUsername());
        assertEquals("STATUS_CHANGED", event.getValue().getAction());
        assertEquals(reviewer.getId(), event.getValue().getActorId());
    }

    @Test
    void riskAcceptanceRequiresReasonAndFutureExpiry() {
        User manager = user(9L, "manager");
        AuditFinding finding = finding(AuditFinding.FindingStatus.OPEN, 7L);
        LocalDate todayUtc = LocalDate.now(ZoneOffset.UTC);
        when(findings.findByUuidAndDeletedFalse(finding.getUuid())).thenReturn(Optional.of(finding));
        when(users.findByUsernameIgnoreCase("manager")).thenReturn(Optional.of(manager));
        var auth = auth("manager", "ROLE_MANAGER");

        assertThrows(IllegalArgumentException.class, () -> service.transition(finding.getUuid(),
                new FindingDtos.StatusRequest(AuditFinding.FindingStatus.RISK_ACCEPTED, "", todayUtc.plusDays(10)),
                auth, "192.0.2.12"));
        assertThrows(IllegalArgumentException.class, () -> service.transition(finding.getUuid(),
                new FindingDtos.StatusRequest(AuditFinding.FindingStatus.RISK_ACCEPTED, "Accepted temporarily", todayUtc),
                auth, "192.0.2.12"));

        var accepted = service.transition(finding.getUuid(),
                new FindingDtos.StatusRequest(AuditFinding.FindingStatus.RISK_ACCEPTED, "Accepted temporarily", todayUtc.plusDays(10)),
                auth, "192.0.2.12");
        assertEquals(AuditFinding.FindingStatus.RISK_ACCEPTED, accepted.status());
        assertEquals(todayUtc.plusDays(10), accepted.riskAcceptanceExpiresOn());
    }

    private static FindingDtos.StatusRequest status(AuditFinding.FindingStatus status) {
        return new FindingDtos.StatusRequest(status, null, null);
    }

    private static UsernamePasswordAuthenticationToken auth(String username, String role) {
        return new UsernamePasswordAuthenticationToken(username, "", List.of(new SimpleGrantedAuthority(role)));
    }

    private static User user(long id, String username) {
        User user = new User(); user.setId(id); user.setUsername(username); user.setEmail(username + "@example.test");
        user.setEnabled(true); user.setRoles(Set.of("ROLE_VIEWER")); return user;
    }

    private static AuditFinding finding(AuditFinding.FindingStatus status, Long ownerId) {
        AuditFinding finding = new AuditFinding(); ReflectionTestUtils.setField(finding, "id", 1L); finding.setUuid(UUID.randomUUID());
        finding.setTitle("Access review overdue"); finding.setDescription("Quarterly review evidence is missing");
        finding.setSeverity(AuditFinding.Severity.HIGH); finding.setStatus(status); finding.setOwnerId(ownerId);
        return finding;
    }
}

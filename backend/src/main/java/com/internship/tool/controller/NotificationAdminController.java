package com.internship.tool.controller;

import com.internship.tool.entity.AuditLogEntry;
import com.internship.tool.entity.NotificationOutbox;
import com.internship.tool.repository.AuditLogRepository;
import com.internship.tool.repository.NotificationOutboxRepository;
import com.internship.tool.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/admin/notifications")
@PreAuthorize("hasRole('ADMIN')")
public class NotificationAdminController {
    private final NotificationOutboxRepository outbox;
    private final AuditLogRepository audit;
    private final UserRepository users;
    public NotificationAdminController(NotificationOutboxRepository outbox, AuditLogRepository audit, UserRepository users) {
        this.outbox = outbox; this.audit = audit; this.users = users;
    }
    @GetMapping
    public Map<String, Object> status() {
        var oldest = outbox.findFirstByStatusOrderByCreatedAtAsc(NotificationOutbox.DeliveryStatus.QUEUED).orElse(null);
        return Map.of("queued", outbox.countByStatus(NotificationOutbox.DeliveryStatus.QUEUED),
                "sent", outbox.countByStatus(NotificationOutbox.DeliveryStatus.SENT),
                "failed", outbox.countByStatus(NotificationOutbox.DeliveryStatus.FAILED),
                "oldestQueuedAt", oldest == null ? "" : oldest.getCreatedAt().toString());
    }
    @PostMapping("/{id}/retry")
    @Transactional
    public ResponseEntity<Void> retry(@PathVariable Long id, Authentication authentication) {
        NotificationOutbox item = outbox.findById(id).orElseThrow(() -> new NoSuchElementException("Notification not found"));
        if (item.getStatus() != NotificationOutbox.DeliveryStatus.FAILED) throw new IllegalArgumentException("Only failed notifications may be retried");
        item.setStatus(NotificationOutbox.DeliveryStatus.QUEUED); item.setAvailableAt(Instant.now()); item.setAttempts(0); item.setLastError(null);
        AuditLogEntry event = new AuditLogEntry(); event.setEntityType("NOTIFICATION"); event.setAction("NOTIFICATION_REQUEUED");
        event.setActorId(users.findByUsernameIgnoreCase(authentication.getName()).orElseThrow().getId());
        event.setNewJson("{\"notificationId\":" + id + "}"); audit.save(event);
        return ResponseEntity.accepted().build();
    }
}

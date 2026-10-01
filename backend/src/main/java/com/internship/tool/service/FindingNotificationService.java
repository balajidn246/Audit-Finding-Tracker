package com.internship.tool.service;

import com.internship.tool.entity.AuditFinding;
import com.internship.tool.entity.NotificationOutbox;
import com.internship.tool.entity.User;
import com.internship.tool.repository.NotificationOutboxRepository;
import com.internship.tool.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FindingNotificationService {
    private final UserRepository users;
    private final NotificationOutboxRepository outbox;
    private final boolean enabled;
    private final String applicationUrl;
    public FindingNotificationService(UserRepository users, NotificationOutboxRepository outbox,
                                      @Value("${notifications.enabled:false}") boolean enabled,
                                      @Value("${notifications.application-url:http://localhost:3000}") String applicationUrl) {
        this.users = users; this.outbox = outbox; this.enabled = enabled; this.applicationUrl = applicationUrl.replaceAll("/+$", "");
    }
    @Transactional
    public void queueOwnerMessage(AuditFinding finding, String eventType) {
        if (!enabled || finding.getOwnerId() == null) return;
        User owner = users.findById(finding.getOwnerId()).filter(User::isEnabled).orElse(null);
        if (owner == null) return;
        String dedupe = finding.getUuid() + ":" + finding.getVersion() + ":" + eventType + ":" + owner.getId();
        if (outbox.existsByDedupeKey(dedupe)) return;
        String phrase = switch (eventType) {
            case "ASSIGNED" -> "A finding has been assigned to you.";
            case "STATUS_CHANGED" -> "A finding assigned to you has changed status to " + finding.getStatus().name() + ".";
            case "EXPIRED" -> "An accepted risk has expired and the finding has reopened.";
            default -> "A finding has been created and assigned to you.";
        };
        NotificationOutbox message = new NotificationOutbox(); message.setDedupeKey(dedupe);
        message.setRecipientEmail(owner.getEmail()); message.setSubject("VerityOps — " + phrase);
        message.setBody(phrase + "\n\nFinding: " + finding.getTitle() + "\nSeverity: " + finding.getSeverity().name()
                + "\nDue date: " + (finding.getDueDate() == null ? "Not set" : finding.getDueDate())
                + "\nRecord: " + applicationUrl + "/?finding=" + finding.getUuid()
                + "\n\nSign in to VerityOps to review the remediation and evidence.");
        outbox.save(message);
    }
}

package com.internship.tool.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.Instant;

@Entity
@Table(name = "notification_outbox", indexes = @Index(name = "idx_notification_outbox_ready", columnList = "status, available_at, id"))
public class NotificationOutbox {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "dedupe_key", nullable = false, unique = true, length = 255, updatable = false) private String dedupeKey;
    @Column(name = "recipient_email", nullable = false, length = 200, updatable = false) private String recipientEmail;
    @Column(nullable = false, length = 255, updatable = false) private String subject;
    @Column(nullable = false, columnDefinition = "text", updatable = false) private String body;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private DeliveryStatus status = DeliveryStatus.QUEUED;
    @Column(nullable = false) private int attempts;
    @Column(name = "available_at", nullable = false) private Instant availableAt;
    @Column(name = "sent_at") private Instant sentAt;
    @Column(name = "last_error", length = 120) private String lastError;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @PrePersist void initialize() { if (availableAt == null) availableAt = Instant.now(); }
    public Long getId() { return id; }
    public String getDedupeKey() { return dedupeKey; } public void setDedupeKey(String v) { dedupeKey = v; }
    public String getRecipientEmail() { return recipientEmail; } public void setRecipientEmail(String v) { recipientEmail = v; }
    public String getSubject() { return subject; } public void setSubject(String v) { subject = v; }
    public String getBody() { return body; } public void setBody(String v) { body = v; }
    public DeliveryStatus getStatus() { return status; } public void setStatus(DeliveryStatus v) { status = v; }
    public int getAttempts() { return attempts; } public void setAttempts(int v) { attempts = v; }
    public Instant getAvailableAt() { return availableAt; } public void setAvailableAt(Instant v) { availableAt = v; }
    public Instant getSentAt() { return sentAt; } public void setSentAt(Instant v) { sentAt = v; }
    public String getLastError() { return lastError; } public void setLastError(String v) { lastError = v; }
    public Instant getCreatedAt() { return createdAt; }
    public enum DeliveryStatus { QUEUED, SENT, FAILED }
}

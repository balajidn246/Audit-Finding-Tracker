package com.internship.tool.service;

import com.internship.tool.entity.NotificationOutbox;
import com.internship.tool.repository.NotificationOutboxRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import java.time.Instant;

@Component
@ConditionalOnProperty(prefix = "notifications", name = "enabled", havingValue = "true")
public class NotificationDispatcher {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(NotificationDispatcher.class);
    private final NotificationOutboxRepository outbox;
    private final JavaMailSender mail;
    private final String from;
    public NotificationDispatcher(NotificationOutboxRepository outbox, JavaMailSender mail,
                                  @Value("${notifications.from:verityops@localhost}") String from) {
        this.outbox = outbox; this.mail = mail; this.from = from;
    }

    @Scheduled(fixedDelayString = "${notifications.poll-interval-ms:30000}")
    @Transactional
    public void dispatch() {
        var messages = outbox.lockReady(Instant.now(), NotificationOutbox.DeliveryStatus.QUEUED, PageRequest.of(0, 20));
        for (NotificationOutbox item : messages) {
            item.setAttempts(item.getAttempts() + 1);
            try {
                SimpleMailMessage email = new SimpleMailMessage(); email.setFrom(from); email.setTo(item.getRecipientEmail());
                email.setSubject(item.getSubject()); email.setText(item.getBody()); mail.send(email);
                item.setStatus(NotificationOutbox.DeliveryStatus.SENT); item.setSentAt(Instant.now()); item.setLastError(null);
            } catch (MailException ex) {
                item.setLastError(ex.getClass().getSimpleName());
                if (item.getAttempts() >= 8) {
                    item.setStatus(NotificationOutbox.DeliveryStatus.FAILED);
                    LOG.error("Notification {} moved to FAILED after {} attempts ({})", item.getId(), item.getAttempts(), ex.getClass().getSimpleName());
                }
                else {
                    long delaySeconds = Math.min(6 * 60 * 60, 30L * (1L << Math.min(item.getAttempts(), 10)));
                    item.setAvailableAt(Instant.now().plusSeconds(delaySeconds));
                    LOG.warn("Notification {} delivery attempt {} failed ({}); retry queued", item.getId(), item.getAttempts(), ex.getClass().getSimpleName());
                }
            }
        }
    }
}

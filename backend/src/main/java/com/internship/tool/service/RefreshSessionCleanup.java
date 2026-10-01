package com.internship.tool.service;

import com.internship.tool.repository.RefreshSessionRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.Instant;

@Component
public class RefreshSessionCleanup {
    private final RefreshSessionRepository sessions;
    public RefreshSessionCleanup(RefreshSessionRepository sessions) { this.sessions = sessions; }
    @Scheduled(cron = "${security.refresh-cleanup-cron:0 23 3 * * *}")
    public void purge() { Instant now = Instant.now(); sessions.purgeExpiredOrRevoked(now, now.minusSeconds(30L * 24 * 60 * 60)); }
}

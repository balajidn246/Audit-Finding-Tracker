package com.internship.tool.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.time.ZoneOffset;

@Component
public class RiskAcceptanceExpiryJob {
    private final RiskAcceptanceExpiryProcessor processor;

    public RiskAcceptanceExpiryJob(RiskAcceptanceExpiryProcessor processor) {
        this.processor = processor;
    }

    @Scheduled(cron = "${security.risk-expiry-cron:0 15 2 * * *}", zone = "UTC")
    public void reopenExpiredAcceptances() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        while (processor.reopenBatch(today) > 0) {
            // Each batch commits independently so large backlogs make steady progress
            // without keeping one long-running transaction or loading every row at once.
        }
    }
}

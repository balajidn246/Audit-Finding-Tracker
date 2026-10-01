package com.internship.tool.controller;

import com.internship.tool.dto.FindingDtos;
import com.internship.tool.repository.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/audit-events")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAuditController {
    private final AuditLogRepository events;
    public AdminAuditController(AuditLogRepository events) { this.events = events; }
    @GetMapping
    public Page<FindingDtos.AuditEventView> list(@RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "50") int size) {
        if (page < 0 || size < 1 || size > 100) throw new IllegalArgumentException("Invalid page or size");
        return events.findByFindingIdIsNullOrderByCreatedAtDesc(PageRequest.of(page, size))
                .map(e -> new FindingDtos.AuditEventView(e.getEventId(), e.getActorId(), e.getEntityType(), e.getAction(),
                        e.getOldJson(), e.getNewJson(), e.getSourceIp(), e.getCreatedAt()));
    }
}

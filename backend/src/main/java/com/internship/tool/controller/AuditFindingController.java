package com.internship.tool.controller;

import com.internship.tool.dto.FindingDtos;
import com.internship.tool.entity.AuditFinding;
import com.internship.tool.service.AuditFindingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/findings")
public class AuditFindingController {
    private final AuditFindingService service;

    public AuditFindingController(AuditFindingService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<FindingDtos.FindingView> create(@Valid @RequestBody FindingDtos.CreateRequest request,
                                                           Authentication auth, HttpServletRequest http) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.create(request, auth, com.internship.tool.security.ClientAddress.from(http)));
    }

    @GetMapping("/{uuid}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','VIEWER')")
    public FindingDtos.FindingView get(@PathVariable UUID uuid) {
        return service.get(uuid);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','VIEWER')")
    public Page<FindingDtos.FindingView> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) AuditFinding.FindingStatus status,
            @RequestParam(required = false) AuditFinding.Severity severity,
            @RequestParam(required = false) Long ownerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "updatedAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String dir) {
        return service.search(q, status, severity, ownerId, page, size, sortBy, dir);
    }

    @PutMapping("/{uuid}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','VIEWER')")
    public FindingDtos.FindingView update(@PathVariable UUID uuid,
                                          @Valid @RequestBody FindingDtos.UpdateRequest request,
                                          Authentication auth, HttpServletRequest http) {
        return service.update(uuid, request, auth, com.internship.tool.security.ClientAddress.from(http));
    }

    @PatchMapping("/{uuid}/status")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','VIEWER')")
    public FindingDtos.FindingView transition(@PathVariable UUID uuid,
                                               @Valid @RequestBody FindingDtos.StatusRequest request,
                                               Authentication auth, HttpServletRequest http) {
        return service.transition(uuid, request, auth, com.internship.tool.security.ClientAddress.from(http));
    }

    @DeleteMapping("/{uuid}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> archive(@PathVariable UUID uuid, Authentication auth, HttpServletRequest http) {
        service.softDelete(uuid, auth, com.internship.tool.security.ClientAddress.from(http));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{uuid}/history")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','VIEWER')")
    public Page<FindingDtos.AuditEventView> history(@PathVariable UUID uuid,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "50") int size) {
        return service.history(uuid, page, size);
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','VIEWER')")
    public Map<String, Long> stats() {
        return service.stats();
    }

    @GetMapping(value = "/report.csv", produces = "text/csv")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<byte[]> export(@RequestParam(required = false) String q,
            @RequestParam(required = false) AuditFinding.FindingStatus status,
            @RequestParam(required = false) AuditFinding.Severity severity,
            @RequestParam(required = false) Long ownerId, Authentication auth, HttpServletRequest http) {
        byte[] csv = service.exportCsv(q, status, severity, ownerId, auth, com.internship.tool.security.ClientAddress.from(http));
        return ResponseEntity.ok().contentType(org.springframework.http.MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .contentLength(csv.length).header("Content-Disposition", "attachment; filename=verityops-findings.csv")
                .cacheControl(org.springframework.http.CacheControl.noStore()).body(csv);
    }
}

package com.internship.tool.controller;

import com.internship.tool.dto.FindingDtos;
import com.internship.tool.entity.AuditFinding;
import com.internship.tool.service.AuditFindingService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/findings")
public class AuditFindingController {

    @Autowired
    private AuditFindingService service;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<?> create(@Valid @RequestBody FindingDtos.CreateRequest req, Authentication auth) {
        var created = service.create(req, auth);
        return ResponseEntity.ok(created);
    }

    @GetMapping("/{uuid}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','VIEWER')")
    public ResponseEntity<?> get(@PathVariable UUID uuid) {
        var f = service.findByUuid(uuid).orElseThrow(() -> new java.util.NoSuchElementException("Not found"));
        return ResponseEntity.ok(f);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','VIEWER')")
    public ResponseEntity<?> list(
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "sortBy", defaultValue = "createdAt") String sortBy,
            @RequestParam(value = "dir", defaultValue = "DESC") String dir
    ) {
        Page<AuditFinding> p = service.search(q, page, size, sortBy, dir);
        return ResponseEntity.ok(p);
    }

    @PutMapping("/{uuid}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<?> update(@PathVariable UUID uuid, @RequestBody FindingDtos.UpdateRequest req, Authentication auth) {
        var u = service.update(uuid, req, auth);
        return ResponseEntity.ok(u);
    }

    @DeleteMapping("/{uuid}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<?> softDelete(@PathVariable UUID uuid, Authentication auth) {
        service.softDelete(uuid, auth);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','VIEWER')")
    public ResponseEntity<?> stats() {
        return ResponseEntity.ok(service.stats());
    }
}

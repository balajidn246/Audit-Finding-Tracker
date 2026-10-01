package com.internship.tool.repository;

import com.internship.tool.entity.AuditLogEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLogEntry, Long> {
    Page<AuditLogEntry> findByFindingIdOrderByCreatedAtDesc(Long findingId, Pageable pageable);
    Page<AuditLogEntry> findByFindingIdIsNullOrderByCreatedAtDesc(Pageable pageable);
}

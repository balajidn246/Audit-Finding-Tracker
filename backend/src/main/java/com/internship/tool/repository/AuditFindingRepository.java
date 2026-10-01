package com.internship.tool.repository;

import com.internship.tool.entity.AuditFinding;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;
import java.time.LocalDate;
import com.internship.tool.entity.AuditFinding.FindingStatus;
import com.internship.tool.entity.AuditFinding.Severity;

public interface AuditFindingRepository extends JpaRepository<AuditFinding, Long>, JpaSpecificationExecutor<AuditFinding> {
    Optional<AuditFinding> findByUuidAndDeletedFalse(UUID uuid);
    Page<AuditFinding> findByDeletedFalse(Pageable pageable);
    long countByDeletedFalse();
    long countByStatusAndDeletedFalse(FindingStatus status);
    long countBySeverityAndDeletedFalse(Severity severity);
    Page<AuditFinding> findByStatusAndRiskAcceptanceExpiresOnLessThanEqualAndDeletedFalse(
            FindingStatus status, LocalDate date, Pageable pageable);
}

package com.internship.tool.repository;

import com.internship.tool.entity.RefreshSession;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;
import java.time.Instant;

public interface RefreshSessionRepository extends JpaRepository<RefreshSession, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<RefreshSession> findByTokenHashAndRevokedAtIsNull(String tokenHash);
    @Modifying @Transactional @Query("delete from RefreshSession s where s.expiresAt < :cutoff or s.revokedAt < :revokedCutoff")
    int purgeExpiredOrRevoked(Instant cutoff, Instant revokedCutoff);
}

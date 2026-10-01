package com.internship.tool.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "refresh_sessions", indexes = @Index(name = "idx_refresh_sessions_user", columnList = "user_id"))
public class RefreshSession {
    @Id @Column(name = "token_hash", length = 64, nullable = false, updatable = false)
    private String tokenHash;
    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;
    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "revoked_at")
    private Instant revokedAt;
    @Version @Column(nullable = false)
    private long version;
    @PrePersist void created() { if (createdAt == null) createdAt = Instant.now(); }
    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getRevokedAt() { return revokedAt; }
    public void setRevokedAt(Instant revokedAt) { this.revokedAt = revokedAt; }
}

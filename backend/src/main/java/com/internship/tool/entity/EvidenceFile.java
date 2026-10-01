package com.internship.tool.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "finding_evidence", indexes = @Index(name = "idx_evidence_finding_created", columnList = "finding_id, created_at"))
public class EvidenceFile {
    @Id @Column(nullable = false, updatable = false) private UUID id;
    @Column(name = "finding_id", nullable = false, updatable = false) private Long findingId;
    @Column(name = "uploaded_by_id", nullable = false, updatable = false) private Long uploadedById;
    @Column(name = "original_name", nullable = false, length = 255, updatable = false) private String originalName;
    @Column(name = "media_type", nullable = false, length = 100, updatable = false) private String mediaType;
    @Column(name = "size_bytes", nullable = false, updatable = false) private long sizeBytes;
    @Column(name = "sha256", nullable = false, length = 64, updatable = false) private String sha256;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @PrePersist void assignId() { if (id == null) id = UUID.randomUUID(); }
    public static EvidenceFile withGeneratedId() {
        EvidenceFile file = new EvidenceFile();
        file.id = UUID.randomUUID();
        return file;
    }
    public UUID getId() { return id; } public Long getFindingId() { return findingId; } public void setFindingId(Long v) { findingId = v; }
    public Long getUploadedById() { return uploadedById; } public void setUploadedById(Long v) { uploadedById = v; }
    public String getOriginalName() { return originalName; } public void setOriginalName(String v) { originalName = v; }
    public String getMediaType() { return mediaType; } public void setMediaType(String v) { mediaType = v; }
    public long getSizeBytes() { return sizeBytes; } public void setSizeBytes(long v) { sizeBytes = v; }
    public String getSha256() { return sha256; } public void setSha256(String v) { sha256 = v; }
    public Instant getCreatedAt() { return createdAt; }
}

package com.internship.tool.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.internship.tool.entity.AuditFinding;
import com.internship.tool.entity.AuditLogEntry;
import com.internship.tool.entity.EvidenceFile;
import com.internship.tool.entity.User;
import com.internship.tool.repository.AuditFindingRepository;
import com.internship.tool.repository.AuditLogRepository;
import com.internship.tool.repository.EvidenceFileRepository;
import com.internship.tool.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

@Service
public class FindingEvidenceService {
    private static final long MAX_BYTES = 10L * 1024 * 1024;
    private final AuditFindingRepository findings; private final EvidenceFileRepository evidence;
    private final AuditLogRepository audit; private final UserRepository users; private final ClamAvScanner scanner;
    private final Path root; private final ObjectMapper json;
    public FindingEvidenceService(AuditFindingRepository findings, EvidenceFileRepository evidence,
                                  AuditLogRepository audit, UserRepository users, ClamAvScanner scanner,
                                  ObjectMapper json, @Value("${files.upload-dir:./var/evidence}") String uploadDir) {
        this.findings = findings; this.evidence = evidence; this.audit = audit; this.users = users;
        this.scanner = scanner; this.json = json; this.root = Path.of(uploadDir).toAbsolutePath().normalize();
    }
    @Transactional
    public EvidenceView upload(UUID uuid, MultipartFile upload, Authentication authentication, String sourceIp) {
        AuditFinding finding = active(uuid); User actor = actor(authentication); authorize(finding, actor, authentication);
        if (upload.isEmpty() || upload.getSize() > MAX_BYTES) throw new IllegalArgumentException("Evidence must be between 1 byte and 10 MB");
        try {
            byte[] bytes = upload.getBytes(); String mediaType = detectType(bytes, upload.getOriginalFilename());
            if (bytes.length == 0 || bytes.length > MAX_BYTES) throw new IllegalArgumentException("Evidence must be between 1 byte and 10 MB");
            scanner.requireClean(bytes);
            String digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
            String originalName = safeName(upload.getOriginalFilename());
            EvidenceFile item = EvidenceFile.withGeneratedId();
            UUID fileId = item.getId();
            Files.createDirectories(root);
            Path destination = root.resolve(fileId.toString()).normalize();
            if (!destination.getParent().equals(root)) throw new IllegalStateException("Invalid evidence storage path");
            Path temporary = Files.createTempFile(root, ".upload-", ".part");
            try { Files.write(temporary, bytes, StandardOpenOption.TRUNCATE_EXISTING); Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE); }
            catch (Exception ex) { Files.deleteIfExists(temporary); throw ex; }
            item.setFindingId(finding.getId()); item.setUploadedById(actor.getId());
            item.setOriginalName(originalName); item.setMediaType(mediaType); item.setSizeBytes(bytes.length); item.setSha256(digest);
            EvidenceFile saved = evidence.save(item);
            AuditLogEntry event = new AuditLogEntry(); event.setFindingId(finding.getId()); event.setActorId(actor.getId());
            event.setAction("EVIDENCE_ADDED"); event.setSourceIp(sourceIp);
            event.setNewJson(json.writeValueAsString(Map.of("evidenceId", fileId, "name", originalName, "sizeBytes", bytes.length, "sha256", digest, "scan", "clean")));
            audit.save(event); return EvidenceView.from(saved);
        } catch (IOException ex) { throw new IllegalStateException("Could not safely store evidence", ex); }
        catch (java.security.NoSuchAlgorithmException ex) { throw new IllegalStateException("SHA-256 is unavailable", ex); }
    }
    @Transactional(readOnly = true)
    public List<EvidenceView> list(UUID uuid, Authentication authentication) { AuditFinding f = active(uuid); authorize(f, actor(authentication), authentication); return evidence.findByFindingIdOrderByCreatedAtDesc(f.getId()).stream().map(EvidenceView::from).toList(); }
    @Transactional(readOnly = true)
    public Download download(UUID uuid, UUID evidenceId, Authentication authentication) {
        AuditFinding f = active(uuid); User actor = actor(authentication); authorize(f, actor, authentication);
        EvidenceFile item = evidence.findByIdAndFindingId(evidenceId, f.getId()).orElseThrow(() -> new NoSuchElementException("Evidence not found"));
        Path path = root.resolve(item.getId().toString()).normalize();
        if (!path.getParent().equals(root) || !Files.isRegularFile(path)) throw new NoSuchElementException("Evidence file not found");
        return new Download(item, new FileSystemResource(path));
    }
    public long count(UUID findingUuid) { return evidence.countByFindingId(active(findingUuid).getId()); }
    private AuditFinding active(UUID uuid) { return findings.findByUuidAndDeletedFalse(uuid).orElseThrow(() -> new NoSuchElementException("Finding not found")); }
    private User actor(Authentication auth) { return users.findByUsernameIgnoreCase(auth.getName()).orElseThrow(() -> new AccessDeniedException("User account unavailable")); }
    private void authorize(AuditFinding finding, User actor, Authentication auth) {
        boolean manager = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch(r -> r.equals("ROLE_ADMIN") || r.equals("ROLE_MANAGER"));
        if (!manager && !Objects.equals(finding.getOwnerId(), actor.getId())) throw new AccessDeniedException("Only the finding owner or audit management can access evidence");
    }
    private String detectType(byte[] b, String name) {
        String lower = name == null ? "" : name.toLowerCase(Locale.ROOT);
        if (b.length >= 5 && b[0]=='%' && b[1]=='P' && b[2]=='D' && b[3]=='F' && b[4]=='-' && lower.endsWith(".pdf")) return "application/pdf";
        if (b.length >= 8 && (b[0]&255)==137 && b[1]=='P' && b[2]=='N' && b[3]=='G' && lower.endsWith(".png")) return "image/png";
        if (b.length >= 3 && (b[0]&255)==255 && (b[1]&255)==216 && (b[2]&255)==255 && (lower.endsWith(".jpg") || lower.endsWith(".jpeg"))) return "image/jpeg";
        throw new IllegalArgumentException("Only valid PDF, PNG, and JPEG evidence files are accepted");
    }
    private String safeName(String name) {
        String normalized = name == null ? "evidence" : name.replace('\\', '/');
        String base = normalized.substring(normalized.lastIndexOf('/') + 1);
        String safe = base.replaceAll("[^A-Za-z0-9._ -]", "_").replaceAll("[. ]+$", "");
        if (safe.isBlank()) safe = "evidence"; return safe.substring(0, Math.min(255, safe.length()));
    }
    public record EvidenceView(UUID id, String originalName, String mediaType, long sizeBytes, String sha256, java.time.Instant createdAt) {
        static EvidenceView from(EvidenceFile f) { return new EvidenceView(f.getId(), f.getOriginalName(), f.getMediaType(), f.getSizeBytes(), f.getSha256(), f.getCreatedAt()); }
    }
    public record Download(EvidenceFile metadata, Resource resource) { }
}

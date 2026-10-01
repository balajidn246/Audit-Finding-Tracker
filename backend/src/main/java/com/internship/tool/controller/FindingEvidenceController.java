package com.internship.tool.controller;

import com.internship.tool.service.FindingEvidenceService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/findings/{uuid}/evidence")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER','VIEWER')")
public class FindingEvidenceController {
    private final FindingEvidenceService service;
    public FindingEvidenceController(FindingEvidenceService service) { this.service = service; }
    @GetMapping public List<FindingEvidenceService.EvidenceView> list(@PathVariable UUID uuid, Authentication authentication) { return service.list(uuid, authentication); }
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FindingEvidenceService.EvidenceView> upload(@PathVariable UUID uuid, @RequestPart("file") MultipartFile file,
            Authentication authentication, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.upload(uuid, file, authentication, com.internship.tool.security.ClientAddress.from(request)));
    }
    @GetMapping("/{evidenceId}")
    public ResponseEntity<Resource> download(@PathVariable UUID uuid, @PathVariable UUID evidenceId, Authentication authentication) {
        var download = service.download(uuid, evidenceId, authentication);
        var item = download.metadata();
        ContentDisposition disposition = ContentDisposition.attachment().filename(item.getOriginalName(), StandardCharsets.UTF_8).build();
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(item.getMediaType())).contentLength(item.getSizeBytes())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString()).header("X-Content-Type-Options", "nosniff")
                .cacheControl(CacheControl.noStore()).body(download.resource());
    }
}

package com.internship.tool.controller;

import com.internship.tool.entity.User;
import com.internship.tool.entity.AuditLogEntry;
import com.internship.tool.repository.AuditLogRepository;
import com.internship.tool.repository.UserRepository;
import com.internship.tool.dto.AuthDtos;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserAdminController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired private AuditLogRepository auditLogs;
    @Autowired private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @GetMapping
    public ResponseEntity<?> list() {
        List<UserSummary> users = userRepository.findAll().stream()
                .map(UserSummary::from)
                .toList();
        return ResponseEntity.ok(users);
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> create(@Valid @RequestBody AuthDtos.RegisterRequest req, Authentication authentication, HttpServletRequest http) {
        if (req.password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            return ResponseEntity.badRequest().body(Map.of("error", "Password must be no longer than 72 UTF-8 bytes"));
        }
        if (userRepository.existsByUsernameIgnoreCase(req.username.trim())) {
            return ResponseEntity.status(409).body(Map.of("error", "Username already exists"));
        }
        if (userRepository.existsByEmailIgnoreCase(req.email.trim())) {
            return ResponseEntity.status(409).body(Map.of("error", "Email already exists"));
        }
        User u = new User();
        u.setUsername(req.username.trim().toLowerCase());
        u.setEmail(req.email.trim().toLowerCase());
        u.setPassword(passwordEncoder.encode(req.password));
        if (req.roles == null || req.roles.isEmpty()) u.setRoles(Set.of("ROLE_VIEWER"));
        else {
            Set<String> roles = req.roles.stream()
                    .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role.toUpperCase())
                    .collect(java.util.stream.Collectors.toSet());
            if (!Set.of("ROLE_ADMIN", "ROLE_MANAGER", "ROLE_VIEWER").containsAll(roles)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Unsupported role"));
            }
            u.setRoles(roles);
        }
        User saved = userRepository.save(u);
        auditUser(authentication, saved, "USER_CREATED", com.internship.tool.security.ClientAddress.from(http));
        return ResponseEntity.ok(UserSummary.from(saved));
    }

    @PostMapping("/{id}/deactivate")
    @Transactional
    public ResponseEntity<?> deactivate(@PathVariable Long id, Authentication authentication, HttpServletRequest http) {
        var opt = userRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        User u = opt.get();
        if (u.getUsername().equalsIgnoreCase(authentication.getName())) return ResponseEntity.badRequest().body(Map.of("error", "You cannot deactivate your own account"));
        if (u.isEnabled() && u.getRoles().contains("ROLE_ADMIN")
                && userRepository.lockEnabledUsersWithRole("ROLE_ADMIN").size() <= 1) {
            return ResponseEntity.badRequest().body(Map.of("error", "The last active administrator cannot be deactivated"));
        }
        u.setEnabled(false);
        userRepository.save(u);
        auditUser(authentication, u, "USER_DEACTIVATED", com.internship.tool.security.ClientAddress.from(http));
        return ResponseEntity.ok(Map.of("status","deactivated"));
    }

    @PostMapping("/{id}/activate")
    @Transactional
    public ResponseEntity<?> activate(@PathVariable Long id, Authentication authentication, HttpServletRequest http) {
        var opt = userRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        User u = opt.get();
        u.setEnabled(true);
        userRepository.save(u);
        auditUser(authentication, u, "USER_ACTIVATED", com.internship.tool.security.ClientAddress.from(http));
        return ResponseEntity.ok(Map.of("status","activated"));
    }

    private void auditUser(Authentication authentication, User changed, String action, String sourceIp) {
        User actor = userRepository.findByUsernameIgnoreCase(authentication.getName()).orElseThrow();
        AuditLogEntry event = new AuditLogEntry(); event.setEntityType("USER"); event.setActorId(actor.getId());
        event.setAction(action); event.setSourceIp(sourceIp);
        try { event.setNewJson(objectMapper.writeValueAsString(Map.of("userId", changed.getId(), "username", changed.getUsername(), "email", changed.getEmail(), "roles", changed.getRoles(), "enabled", changed.isEnabled()))); }
        catch (com.fasterxml.jackson.core.JsonProcessingException ex) { throw new IllegalStateException("Could not write user audit event", ex); }
        auditLogs.save(event);
    }

    public record UserSummary(Long id, String username, String email, Set<String> roles,
                              boolean enabled, java.time.Instant createdAt) {
        static UserSummary from(User user) {
            return new UserSummary(user.getId(), user.getUsername(), user.getEmail(),
                    user.getRoles(), user.isEnabled(), user.getCreatedAt());
        }
    }
}

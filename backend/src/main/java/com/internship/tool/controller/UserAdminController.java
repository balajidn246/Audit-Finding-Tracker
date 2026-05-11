package com.internship.tool.controller;

import com.internship.tool.entity.User;
import com.internship.tool.repository.UserRepository;
import com.internship.tool.dto.AuthDtos;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping
    public ResponseEntity<?> list() {
        List<User> users = userRepository.findAll();
        return ResponseEntity.ok(users);
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody AuthDtos.RegisterRequest req) {
        User u = new User();
        u.setUsername(req.username.toLowerCase());
        u.setEmail(req.email.toLowerCase());
        u.setPassword(passwordEncoder.encode(req.password));
        if (req.roles == null || req.roles.isEmpty()) u.setRoles(Set.of("ROLE_VIEWER"));
        else u.setRoles(req.roles);
        userRepository.save(u);
        return ResponseEntity.ok(Map.of("status","created"));
    }

    @PostMapping("/{id}/deactivate")
    public ResponseEntity<?> deactivate(@PathVariable Long id) {
        var opt = userRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        User u = opt.get();
        u.setEnabled(false);
        userRepository.save(u);
        return ResponseEntity.ok(Map.of("status","deactivated"));
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<?> activate(@PathVariable Long id) {
        var opt = userRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        User u = opt.get();
        u.setEnabled(true);
        userRepository.save(u);
        return ResponseEntity.ok(Map.of("status","activated"));
    }
}

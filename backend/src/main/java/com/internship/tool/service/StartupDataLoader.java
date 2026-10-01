package com.internship.tool.service;

import com.internship.tool.entity.User;
import com.internship.tool.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import org.springframework.beans.factory.annotation.Value;

import java.util.Set;

@Component
public class StartupDataLoader implements ApplicationRunner {

    @Value("${INITIAL_ADMIN_USERNAME:}")
    private String initialAdminUsername;

    @Value("${INITIAL_ADMIN_EMAIL:}")
    private String initialAdminEmail;

    @Value("${INITIAL_ADMIN_PASSWORD:}")
    private String initialAdminPassword;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (userRepository.count() == 0) {
            if (initialAdminUsername.isBlank() || initialAdminEmail.isBlank() || initialAdminPassword.isBlank()) {
                throw new IllegalStateException(
                        "An empty database requires INITIAL_ADMIN_USERNAME, INITIAL_ADMIN_EMAIL, and INITIAL_ADMIN_PASSWORD");
            }
            if (initialAdminPassword.length() < 16) {
                throw new IllegalStateException("INITIAL_ADMIN_PASSWORD must be at least 16 characters long");
            }
            if (initialAdminPassword.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
                throw new IllegalStateException("INITIAL_ADMIN_PASSWORD must be no longer than 72 UTF-8 bytes");
            }
            User admin = new User();
            admin.setUsername(initialAdminUsername.trim().toLowerCase());
            admin.setEmail(initialAdminEmail.trim().toLowerCase());
            admin.setPassword(passwordEncoder.encode(initialAdminPassword));
            admin.setRoles(Set.of("ROLE_ADMIN"));
            admin.setEnabled(true);
            userRepository.save(admin);
        }
    }
}

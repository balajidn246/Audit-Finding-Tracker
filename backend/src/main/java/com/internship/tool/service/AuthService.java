package com.internship.tool.service;

import com.internship.tool.dto.AuthDtos;
import com.internship.tool.entity.User;
import com.internship.tool.repository.UserRepository;
import com.internship.tool.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Transactional
    public AuthDtos.TokenResponse register(AuthDtos.RegisterRequest req) {
        if (userRepository.existsByUsernameIgnoreCase(req.username)) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (userRepository.existsByEmailIgnoreCase(req.email)) {
            throw new IllegalArgumentException("Email already exists");
        }

        User u = new User();
        u.setUsername(req.username.trim().toLowerCase());
        u.setEmail(req.email.trim().toLowerCase());
        u.setPassword(passwordEncoder.encode(req.password));
        if (req.roles == null || req.roles.isEmpty()) {
            u.setRoles(Set.of("ROLE_VIEWER"));
        } else {
            // Sanitize roles to expected prefixes
            Set<String> roles = req.roles.stream()
                    .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r.toUpperCase())
                    .collect(java.util.stream.Collectors.toSet());
            u.setRoles(roles);
        }
        userRepository.save(u);

        String accessToken = jwtTokenProvider.generateAccessToken(u.getUsername(), u.getRoles());
        String refreshToken = jwtTokenProvider.generateRefreshToken(u.getUsername());

        return new AuthDtos.TokenResponse(accessToken, refreshToken, jwtTokenProvider.accessTokenValidityInMs);
    }

    public AuthDtos.TokenResponse login(AuthDtos.LoginRequest req) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(req.username, req.password));
        User u = userRepository.findByUsernameIgnoreCase(req.username).orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));
        String accessToken = jwtTokenProvider.generateAccessToken(u.getUsername(), u.getRoles());
        String refreshToken = jwtTokenProvider.generateRefreshToken(u.getUsername());
        return new AuthDtos.TokenResponse(accessToken, refreshToken, jwtTokenProvider.accessTokenValidityInMs);
    }

    public AuthDtos.TokenResponse refresh(AuthDtos.RefreshRequest request) {
        String token = request.refreshToken;
        if (!jwtTokenProvider.validateToken(token)) {
            throw new IllegalArgumentException("Invalid refresh token");
        }
        String username = jwtTokenProvider.getUsernameFromToken(token);
        User u = userRepository.findByUsernameIgnoreCase(username).orElseThrow(() -> new IllegalArgumentException("User not found"));
        String accessToken = jwtTokenProvider.generateAccessToken(u.getUsername(), u.getRoles());
        String refreshToken = jwtTokenProvider.generateRefreshToken(u.getUsername());
        return new AuthDtos.TokenResponse(accessToken, refreshToken, jwtTokenProvider.accessTokenValidityInMs);
    }
}

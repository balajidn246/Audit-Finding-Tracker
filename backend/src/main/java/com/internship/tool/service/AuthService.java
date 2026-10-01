package com.internship.tool.service;

import com.internship.tool.dto.AuthDtos;
import com.internship.tool.entity.RefreshSession;
import com.internship.tool.entity.User;
import com.internship.tool.repository.RefreshSessionRepository;
import com.internship.tool.repository.UserRepository;
import com.internship.tool.security.JwtTokenProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Set;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokens;
    private final AuthenticationManager authenticationManager;
    private final RefreshSessionRepository sessions;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtTokenProvider tokens,
                       AuthenticationManager authenticationManager, RefreshSessionRepository sessions) {
        this.userRepository = userRepository; this.passwordEncoder = passwordEncoder; this.tokens = tokens;
        this.authenticationManager = authenticationManager; this.sessions = sessions;
    }

    @Transactional
    public AuthDtos.TokenResponse register(AuthDtos.RegisterRequest req) {
        requireBcryptLength(req.password);
        if (userRepository.existsByUsernameIgnoreCase(req.username)) throw new IllegalArgumentException("Username already exists");
        if (userRepository.existsByEmailIgnoreCase(req.email)) throw new IllegalArgumentException("Email already exists");
        User user = new User(); user.setUsername(req.username.trim().toLowerCase()); user.setEmail(req.email.trim().toLowerCase());
        user.setPassword(passwordEncoder.encode(req.password)); user.setRoles(Set.of("ROLE_VIEWER"));
        return issue(userRepository.save(user));
    }

    @Transactional
    public AuthDtos.TokenResponse login(AuthDtos.LoginRequest req) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(req.username, req.password));
        User user = userRepository.findByUsernameIgnoreCase(req.username).orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));
        if (!user.isEnabled()) throw new IllegalArgumentException("Invalid credentials");
        return issue(user);
    }

    @Transactional
    public AuthDtos.TokenResponse refresh(String refreshToken) {
        if (refreshToken == null || !tokens.validateToken(refreshToken, "refresh")) throw new IllegalArgumentException("Invalid refresh session");
        String hash = hash(tokens.getTokenId(refreshToken));
        RefreshSession oldSession = sessions.findByTokenHashAndRevokedAtIsNull(hash)
                .filter(session -> session.getExpiresAt().isAfter(java.time.Instant.now()))
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired refresh session"));
        User user = userRepository.findById(oldSession.getUserId()).filter(User::isEnabled)
                .orElseThrow(() -> new IllegalArgumentException("User account is unavailable"));
        oldSession.setRevokedAt(java.time.Instant.now());
        return issue(user);
    }

    @Transactional
    public void revoke(String refreshToken) {
        if (refreshToken == null || !tokens.validateToken(refreshToken, "refresh")) return;
        sessions.findByTokenHashAndRevokedAtIsNull(hash(tokens.getTokenId(refreshToken)))
                .ifPresent(session -> session.setRevokedAt(java.time.Instant.now()));
    }

    private AuthDtos.TokenResponse issue(User user) {
        String access = tokens.generateAccessToken(user.getUsername(), user.getRoles());
        String refresh = tokens.generateRefreshToken(user.getUsername());
        RefreshSession session = new RefreshSession(); session.setTokenHash(hash(tokens.getTokenId(refresh)));
        session.setUserId(user.getId()); session.setExpiresAt(tokens.getExpiration(refresh)); sessions.save(session);
        return new AuthDtos.TokenResponse(access, refresh, tokens.getAccessTokenValidityInMs());
    }

    private String hash(String tokenId) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(tokenId.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException ex) { throw new IllegalStateException("SHA-256 is unavailable", ex); }
    }
    private void requireBcryptLength(String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) throw new IllegalArgumentException("Password must be no longer than 72 UTF-8 bytes");
    }
}

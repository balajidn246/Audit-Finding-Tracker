package com.internship.tool.controller;

import com.internship.tool.dto.AuthDtos;
import com.internship.tool.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final String COOKIE_NAME = "verityops_refresh";
    private final AuthService authService;
    @Value("${security.refresh-cookie-secure:true}") private boolean secureCookie;
    @Value("${jwt.refresh-token-expiration-ms:604800000}") private long refreshLifetimeMs;
    @Value("${security.cors.allowed-origins:http://localhost:5173}") private String allowedOrigins;
    @Value("${security.registration-enabled:false}") private boolean registrationEnabled;
    public AuthController(AuthService authService) { this.authService = authService; }

    @PostMapping("/register")
    public ResponseEntity<AuthDtos.TokenResponse> register(@Valid @RequestBody AuthDtos.RegisterRequest request) {
        if (!registrationEnabled) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND);
        return tokenResponse(authService.register(request));
    }
    @PostMapping("/login")
    public ResponseEntity<AuthDtos.TokenResponse> login(@Valid @RequestBody AuthDtos.LoginRequest request) {
        return tokenResponse(authService.login(request));
    }
    @PostMapping("/refresh")
    public ResponseEntity<AuthDtos.TokenResponse> refresh(@CookieValue(name = COOKIE_NAME, required = false) String token,
                                                           @RequestHeader(value = "Origin", required = false) String origin) {
        verifyBrowserOrigin(origin);
        return tokenResponse(authService.refresh(token));
    }
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(name = COOKIE_NAME, required = false) String token,
                                       @RequestHeader(value = "Origin", required = false) String origin) {
        verifyBrowserOrigin(origin);
        authService.revoke(token);
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookie(null, 0).toString()).build();
    }
    @GetMapping("/ping")
    public ResponseEntity<?> ping() { return ResponseEntity.ok(java.util.Map.of("status", "ok")); }

    private ResponseEntity<AuthDtos.TokenResponse> tokenResponse(AuthDtos.TokenResponse tokens) {
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie(tokens.refreshToken, refreshLifetimeMs).toString()).body(tokens);
    }
    private ResponseCookie cookie(String value, long ageSecondsMs) {
        return ResponseCookie.from(COOKIE_NAME, value == null ? "" : value).httpOnly(true).secure(secureCookie)
                .sameSite("Strict").path("/api/auth").maxAge(java.time.Duration.ofMillis(ageSecondsMs)).build();
    }
    private void verifyBrowserOrigin(String origin) {
        boolean allowed = origin != null && java.util.Arrays.stream(allowedOrigins.split(","))
                .map(String::trim).anyMatch(origin::equals);
        if (!allowed) throw new org.springframework.security.access.AccessDeniedException("Request origin is not allowed");
    }
}

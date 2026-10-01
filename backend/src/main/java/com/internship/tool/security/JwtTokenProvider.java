package com.internship.tool.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
public class JwtTokenProvider {
    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.access-token-expiration-ms}")
    private long accessTokenValidityInMs;

    @Value("${jwt.refresh-token-expiration-ms}")
    private long refreshTokenValidityInMs;

    private SecretKey key;

    @PostConstruct
    public void init() {
        if (jwtSecret == null || jwtSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT_SECRET must contain at least 32 bytes of unpredictable secret material");
        }
        if (accessTokenValidityInMs < 60_000 || refreshTokenValidityInMs <= accessTokenValidityInMs) {
            throw new IllegalStateException("JWT token lifetimes are invalid");
        }
        key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(String username, Set<String> roles) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(username)
                .claim("type", "access")
                .claim("roles", roles)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(accessTokenValidityInMs)))
                .signWith(key)
                .compact();
    }

    public String generateRefreshToken(String username) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(username)
                .id(UUID.randomUUID().toString())
                .claim("type", "refresh")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(refreshTokenValidityInMs)))
                .signWith(key)
                .compact();
    }

    public String getTokenId(String token) { return parse(token).getId(); }
    public Instant getExpiration(String token) { return parse(token).getExpiration().toInstant(); }
    public long getAccessTokenValidityInMs() { return accessTokenValidityInMs; }

    public boolean validateToken(String token, String expectedType) {
        try {
            Claims claims = parse(token);
            return claims.getExpiration() != null
                    && claims.getExpiration().after(new Date())
                    && expectedType.equals(claims.get("type", String.class));
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    public String getUsernameFromToken(String token) {
        return parse(token).getSubject();
    }

    public Set<String> getRolesFromToken(String token) {
        Object roles = parse(token).get("roles");
        if (!(roles instanceof List<?> roleList)) return Set.of();
        return roleList.stream().filter(String.class::isInstance).map(String.class::cast).collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}

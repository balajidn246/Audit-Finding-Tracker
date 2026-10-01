package com.internship.tool.config;

import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

@Component
public class RateLimitFilter extends OncePerRequestFilter {
    private static final long WINDOW_SECONDS = 60;
    private static final DefaultRedisScript<Long> INCREMENT_WINDOW = new DefaultRedisScript<>(
            "local count = redis.call('INCR', KEYS[1]); " +
                    "if count == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]); end; " +
                    "return count", Long.class);

    private final StringRedisTemplate redis;

    public RateLimitFilter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return !"POST".equalsIgnoreCase(request.getMethod())
                || !(path.equals("/api/auth/login") || path.equals("/api/auth/register")
                     || path.equals("/api/auth/refresh"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getServletPath();
        long limit = path.endsWith("/login") ? 10 : path.endsWith("/register") ? 5 : 30;
        String trustedClient = com.internship.tool.security.ClientAddress.from(request);
        String key = "auth-rate:" + path.substring(path.lastIndexOf('/') + 1) + ":" + digest(trustedClient);
        try {
            Long count = redis.execute(INCREMENT_WINDOW, List.of(key), Long.toString(WINDOW_SECONDS));
            if (count == null || count > limit) {
                response.setStatus(429);
                response.setHeader(HttpHeaders.RETRY_AFTER, Long.toString(WINDOW_SECONDS));
                response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
                response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
                response.getWriter().write("{\"title\":\"Too many requests\",\"status\":429,\"detail\":\"Try again later.\"}");
                return;
            }
        } catch (DataAccessException ex) {
            // Authentication endpoints do not fail open when the distributed abuse-control store is unavailable.
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            response.setHeader(HttpHeaders.RETRY_AFTER, "30");
            response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            response.getWriter().write("{\"title\":\"Authentication temporarily unavailable\",\"status\":503}");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private String digest(String value) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }
}

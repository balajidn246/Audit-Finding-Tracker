package com.internship.tool.config;

import com.github.bucket4j.Bandwidth;
import com.github.bucket4j.Bucket;
import com.github.bucket4j.Bucket4j;
import com.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    private Bucket createBucket() {
        Refill refill = Refill.greedy(100, Duration.ofMinutes(1));
        Bandwidth limit = Bandwidth.classic(100, refill);
        return Bucket4j.builder().addLimit(limit).build();
    }

    private Bucket resolveBucket(HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        return cache.computeIfAbsent(ip, k -> createBucket());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        Bucket bucket = resolveBucket(request);
        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Too many requests\"}");
        }
    }
}

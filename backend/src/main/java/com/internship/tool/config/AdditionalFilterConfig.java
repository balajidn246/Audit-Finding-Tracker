package com.internship.tool.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@Order(1)
public class AdditionalFilterConfig {

    @Autowired
    private RateLimitFilter rateLimitFilter;

    public SecurityFilterChain configure(HttpSecurity http) throws Exception {
        // RateLimitFilter is a OncePerRequestFilter and is registered as a bean; SecurityConfig already adds jwt filter
        // Nothing to configure here programmatically; kept for clarity in production to register filters with order
        return http.build();
    }
}

package com.internship.tool.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

public final class ClientAddress {
    private ClientAddress() { }
    /** X-Real-IP is overwritten by the only public ingress (the bundled Nginx proxy). */
    public static String from(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Real-IP");
        return StringUtils.hasText(forwarded) && forwarded.length() <= 64 ? forwarded.trim() : request.getRemoteAddr();
    }
}

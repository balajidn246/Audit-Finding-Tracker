package com.internship.tool.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;

public class AuthDtos {

    public static class RegisterRequest {
        @NotBlank
        @Size(min = 3, max = 100)
        public String username;

        @NotBlank
        @Email
        public String email;

        @NotBlank
        @Size(min = 8, max = 100)
        public String password;

        public Set<String> roles;
    }

    public static class LoginRequest {
        @NotBlank
        public String username;

        @NotBlank
        public String password;
    }

    public static class TokenResponse {
        public String accessToken;
        public String refreshToken;
        public long expiresInMs;
        public String tokenType = "Bearer";

        public TokenResponse(String accessToken, String refreshToken, long expiresInMs) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.expiresInMs = expiresInMs;
        }
    }

    public static class RefreshRequest {
        @NotBlank
        public String refreshToken;
    }
}

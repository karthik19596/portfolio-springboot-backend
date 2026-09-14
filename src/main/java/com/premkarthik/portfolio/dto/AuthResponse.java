package com.premkarthik.portfolio.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String refreshToken;
    private String username;
    private String role;
    private String message;

    public AuthResponse(String token, String username, String role, String message) {
        this(token, null, username, role, message);
    }
}

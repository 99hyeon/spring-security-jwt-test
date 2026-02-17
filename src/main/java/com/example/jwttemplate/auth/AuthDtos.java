package com.example.jwttemplate.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class AuthDtos {

    public record LoginRequest(
        @Email @NotBlank String email,
        @NotBlank String password
    ) {}

    public record LoginResponse(
        Long userId,
        String email,
        String role
    ) {}

    public record MeResponse(
        Long userId,
        String email,
        String role
    ) {}
}

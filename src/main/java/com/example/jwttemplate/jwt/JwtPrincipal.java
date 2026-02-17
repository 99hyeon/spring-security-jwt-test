package com.example.jwttemplate.jwt;

public record JwtPrincipal(
    Long userId,
    String email,
    String role
) {}

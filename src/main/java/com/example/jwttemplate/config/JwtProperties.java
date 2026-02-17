package com.example.jwttemplate.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
    String secret,
    String issuer,
    long accessTtlSeconds,
    long refreshTtlSeconds
) {}

package com.example.jwttemplate.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security")
public record SecurityProperties(
    Cors cors,
    Cookie cookie
) {
    public record Cors(String allowedOrigins) {}

    public record Cookie(
        String refreshName,
        boolean secure,
        String sameSite,
        String path
    ) {}
}

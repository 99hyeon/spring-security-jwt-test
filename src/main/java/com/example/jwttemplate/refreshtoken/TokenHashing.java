package com.example.jwttemplate.refreshtoken;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public final class TokenHashing {
    private TokenHashing() {}

    public static String sha256Hex(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("hash_failed", e);
        }
    }
}

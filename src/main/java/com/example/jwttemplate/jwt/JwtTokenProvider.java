package com.example.jwttemplate.jwt;

import com.example.jwttemplate.config.JwtProperties;
import com.example.jwttemplate.user.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtProperties props;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(props.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(Long userId, String email, UserRole role) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(props.accessTtlSeconds());

        return Jwts.builder()
            .issuer(props.issuer())
            .subject(String.valueOf(userId))
            .id(UUID.randomUUID().toString())
            .claim("typ", JwtTokenType.ACCESS.name())
            .claim("email", email)
            .claim("role", role.name())
            .issuedAt(Date.from(now))
            .expiration(Date.from(exp))
            .signWith(key())
            .compact();
    }

    public String createRefreshToken(Long userId) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(props.refreshTtlSeconds());

        return Jwts.builder()
            .issuer(props.issuer())
            .subject(String.valueOf(userId))
            .id(UUID.randomUUID().toString())
            .claim("typ", JwtTokenType.REFRESH.name())
            .issuedAt(Date.from(now))
            .expiration(Date.from(exp))
            .signWith(key())
            .compact();
    }

    public Claims parseAndValidate(String token) {
        try {
            return Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        } catch (Exception e) {
            throw new JwtException("invalid_token");
        }
    }

    public JwtTokenType tokenType(Claims claims) {
        String typ = claims.get("typ", String.class);
        if (typ == null) throw new JwtException("invalid_token_type");
        try {
            return JwtTokenType.valueOf(typ);
        } catch (Exception e) {
            throw new JwtException("invalid_token_type");
        }
    }

    public Long userId(Claims claims) {
        try {
            return Long.parseLong(claims.getSubject());
        } catch (Exception e) {
            throw new JwtException("invalid_subject");
        }
    }

    public String email(Claims claims) {
        return claims.get("email", String.class);
    }

    public UserRole role(Claims claims) {
        String role = claims.get("role", String.class);
        if (role == null) throw new JwtException("missing_role");
        return UserRole.valueOf(role);
    }
}

package com.example.jwttemplate.auth;

import com.example.jwttemplate.config.SecurityProperties;
import com.example.jwttemplate.global.ApiResponse;
import com.example.jwttemplate.jwt.JwtException;
import com.example.jwttemplate.jwt.JwtTokenProvider;
import com.example.jwttemplate.jwt.JwtTokenType;
import com.example.jwttemplate.refreshtoken.RefreshToken;
import com.example.jwttemplate.refreshtoken.RefreshTokenRepository;
import com.example.jwttemplate.refreshtoken.TokenHashing;
import com.example.jwttemplate.user.User;
import com.example.jwttemplate.user.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final SecurityProperties securityProperties;

    public ApiResponse<AuthDtos.LoginResponse> login(
        AuthDtos.LoginRequest req,
        HttpServletResponse response
    ) {
        User user = userRepository.findByEmail(req.email())
            .orElseThrow(() -> new IllegalArgumentException("invalid_credentials"));

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("invalid_credentials");
        }

        String access = tokenProvider.createAccessToken(user.getId(), user.getEmail(), user.getRole());
        String refresh = tokenProvider.createRefreshToken(user.getId());

        persistRefresh(user.getId(), refresh);

        setAccessHeader(response, access);
        setRefreshCookie(response, refresh, tokenProvider.parseAndValidate(refresh).getExpiration().toInstant());

        return ApiResponse.ok("login_ok",
            new AuthDtos.LoginResponse(user.getId(), user.getEmail(), user.getRole().name())
        );
    }

    public ApiResponse<Void> refresh(HttpServletResponse response, String refreshTokenFromCookie) {
        if (refreshTokenFromCookie == null || refreshTokenFromCookie.isBlank()) {
            throw new JwtException("missing_refresh_cookie");
        }

        Claims claims = tokenProvider.parseAndValidate(refreshTokenFromCookie);
        if (tokenProvider.tokenType(claims) != JwtTokenType.REFRESH) {
            throw new JwtException("invalid_refresh_token");
        }

        Long userId = tokenProvider.userId(claims);

        String hash = TokenHashing.sha256Hex(refreshTokenFromCookie);
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash)
            .orElseThrow(() -> new JwtException("refresh_not_found"));

        if (stored.isRevoked()) throw new JwtException("refresh_revoked");
        if (stored.getExpiresAt().isBefore(Instant.now())) throw new JwtException("refresh_expired");

        // Rotate refresh
        stored.revoke();
        refreshTokenRepository.save(stored);

        String newRefresh = tokenProvider.createRefreshToken(userId);
        persistRefresh(userId, newRefresh);

        User user = userRepository.findById(userId).orElseThrow(() -> new JwtException("user_not_found"));
        String newAccess = tokenProvider.createAccessToken(user.getId(), user.getEmail(), user.getRole());

        setAccessHeader(response, newAccess);
        setRefreshCookie(response, newRefresh, tokenProvider.parseAndValidate(newRefresh).getExpiration().toInstant());

        return ApiResponse.ok("refresh_ok");
    }

    public ApiResponse<Void> logout(HttpServletResponse response, String refreshTokenFromCookie) {
        if (refreshTokenFromCookie != null && !refreshTokenFromCookie.isBlank()) {
            String hash = TokenHashing.sha256Hex(refreshTokenFromCookie);
            refreshTokenRepository.findByTokenHash(hash).ifPresent(rt -> {
                rt.revoke();
                refreshTokenRepository.save(rt);
            });
        }

        clearRefreshCookie(response);
        return ApiResponse.ok("logout_ok");
    }

    private void persistRefresh(Long userId, String refresh) {
        Instant exp = tokenProvider.parseAndValidate(refresh).getExpiration().toInstant();
        refreshTokenRepository.save(new RefreshToken(TokenHashing.sha256Hex(refresh), userId, exp));
    }

    private void setAccessHeader(HttpServletResponse response, String accessToken) {
        response.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
    }

    private void setRefreshCookie(HttpServletResponse response, String refreshToken, Instant expiresAt) {
        SecurityProperties.Cookie c = securityProperties.cookie();
        long maxAge = Math.max(0, expiresAt.getEpochSecond() - Instant.now().getEpochSecond());

        ResponseCookie cookie = ResponseCookie.from(c.refreshName(), refreshToken)
            .httpOnly(true)
            .secure(c.secure())
            .sameSite(c.sameSite())
            .path(c.path())
            .maxAge(maxAge)
            .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        SecurityProperties.Cookie c = securityProperties.cookie();

        ResponseCookie cookie = ResponseCookie.from(c.refreshName(), "")
            .httpOnly(true)
            .secure(c.secure())
            .sameSite(c.sameSite())
            .path(c.path())
            .maxAge(0)
            .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}

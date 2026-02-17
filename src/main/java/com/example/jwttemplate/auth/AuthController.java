package com.example.jwttemplate.auth;

import com.example.jwttemplate.global.ApiResponse;
import com.example.jwttemplate.jwt.JwtPrincipal;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/auth/login")
    public ApiResponse<AuthDtos.LoginResponse> login(
        @Valid @RequestBody AuthDtos.LoginRequest request,
        HttpServletResponse response
    ) {
        return authService.login(request, response);
    }

    @PostMapping("/auth/refresh")
    public ApiResponse<Void> refresh(
        @CookieValue(name = "refresh_token", required = false) String refreshToken,
        HttpServletResponse response
    ) {
        return authService.refresh(response, refreshToken);
    }

    @PostMapping("/auth/logout")
    public ApiResponse<Void> logout(
        @CookieValue(name = "refresh_token", required = false) String refreshToken,
        HttpServletResponse response
    ) {
        return authService.logout(response, refreshToken);
    }

    @GetMapping("/me")
    public ApiResponse<AuthDtos.MeResponse> me(@AuthenticationPrincipal JwtPrincipal principal) {
        return ApiResponse.ok("me_ok",
            new AuthDtos.MeResponse(principal.userId(), principal.email(), principal.role())
        );
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/ping")
    public ApiResponse<String> adminPing() {
        return ApiResponse.ok("admin_ok", "pong");
    }
}

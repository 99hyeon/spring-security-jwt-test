package com.example.jwttemplate;

import com.example.jwttemplate.refreshtoken.RefreshTokenRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthFlowTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    RefreshTokenRepository refreshTokenRepository;

    @Test
    @DisplayName("로그인 성공 시: Authorization 헤더(access) + Set-Cookie(refresh)가 내려온다")
    void login_sets_refresh_cookie_and_access_header() throws Exception {
        //given
        String loginBody = "{\"email\":\"user@example.com\",\"password\":\"password1234\"}";

        //when & then
        MvcResult result = mvc.perform(post("/api/auth/login")
                .contentType("application/json")
                .content(loginBody))
            .andExpect(status().isOk())
            .andExpect(header().exists(HttpHeaders.AUTHORIZATION))
            .andExpect(header().string(HttpHeaders.SET_COOKIE,
                org.hamcrest.Matchers.containsString("refresh_token=")))
            .andReturn();

        assertThat(result.getResponse().getHeader(HttpHeaders.AUTHORIZATION)).startsWith("Bearer ");
        assertThat(refreshTokenRepository.count()).isGreaterThan(0);
    }

    @Test
    @DisplayName("인증 없이 보호 API 호출하면: 접근이 거부된다(기본 설정 기준 403)")
    void protected_endpoint_requires_access_token() throws Exception {
        //when & then
        mvc.perform(get("/api/me"))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Access Token이 있으면: 보호 API(/api/me)에 접근할 수 있다")
    void access_token_allows_protected_endpoint() throws Exception {
        //given
        String loginBody = "{\"email\":\"user@example.com\",\"password\":\"password1234\"}";

        //when & then
        MvcResult login = mvc.perform(post("/api/auth/login")
                .contentType("application/json")
                .content(loginBody))
            .andExpect(status().isOk())
            .andReturn();

        String access = login.getResponse().getHeader(HttpHeaders.AUTHORIZATION);

        mvc.perform(get("/api/me").header(HttpHeaders.AUTHORIZATION, access))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("me_ok"))
            .andExpect(jsonPath("$.data.email").value("user@example.com"));
    }

    @Test
    @DisplayName("Refresh Token으로 재발급하면: 새 Access/Refresh가 발급되고 Refresh는 로테이션된다")
    void refresh_rotates_tokens() throws Exception {
        //given
        String loginBody = "{\"email\":\"user@example.com\",\"password\":\"password1234\"}";

        //when & then
        MvcResult login = mvc.perform(post("/api/auth/login")
                .contentType("application/json")
                .content(loginBody))
            .andExpect(status().isOk())
            .andReturn();
        String setCookie = login.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).contains("refresh_token=");

        Cookie refreshCookie = login.getResponse().getCookie("refresh_token");
        MvcResult refreshed = mvc.perform(post("/api/auth/refresh")
                .cookie(refreshCookie))
            .andExpect(status().isOk())
            .andExpect(header().exists(HttpHeaders.AUTHORIZATION))
            .andExpect(header().string(HttpHeaders.SET_COOKIE,
                org.hamcrest.Matchers.containsString("refresh_token=")))
            .andReturn();

        String newSetCookie = refreshed.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertThat(newSetCookie).contains("refresh_token=");
    }

    @Test
    @DisplayName("로그아웃하면: refresh 쿠키가 삭제(Max-Age=0)된다")
    void logout_clears_cookie() throws Exception {
        //given
        String loginBody = "{\"email\":\"user@example.com\",\"password\":\"password1234\"}";

        //when & then
        MvcResult login = mvc.perform(post("/api/auth/login")
                .contentType("application/json")
                .content(loginBody))
            .andExpect(status().isOk())
            .andReturn();

        Cookie refreshCookie = login.getResponse().getCookie("refresh_token");

        mvc.perform(post("/api/auth/logout").cookie(refreshCookie))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.SET_COOKIE,
                org.hamcrest.Matchers.containsString("Max-Age=0")));
    }
}

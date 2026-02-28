package com.example.jwttemplate;

import com.example.jwttemplate.refreshtoken.RefreshTokenRepository;
import jakarta.servlet.http.Cookie;
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
    void login_sets_refresh_cookie_and_access_header() throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/login")
                .contentType("application/json")
                .content("{\"email\":\"user@example.com\",\"password\":\"password1234\"}"))
            .andExpect(status().isOk())
            .andExpect(header().exists(HttpHeaders.AUTHORIZATION))
            .andExpect(header().string(HttpHeaders.SET_COOKIE,
                org.hamcrest.Matchers.containsString("refresh_token=")))
            .andReturn();

        assertThat(result.getResponse().getHeader(HttpHeaders.AUTHORIZATION)).startsWith("Bearer ");
        assertThat(refreshTokenRepository.count()).isGreaterThan(0);
    }

    @Test
    void protected_endpoint_requires_access_token() throws Exception {
        mvc.perform(get("/api/me"))
            .andExpect(
                status().isForbidden()); // Spring Security default: 403 when unauthenticated and no entry point customized
    }

    @Test
    void access_token_allows_protected_endpoint() throws Exception {
        MvcResult login = mvc.perform(post("/api/auth/login")
                .contentType("application/json")
                .content("{\"email\":\"user@example.com\",\"password\":\"password1234\"}"))
            .andExpect(status().isOk())
            .andReturn();

        String access = login.getResponse().getHeader(HttpHeaders.AUTHORIZATION);

        mvc.perform(get("/api/me").header(HttpHeaders.AUTHORIZATION, access))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("me_ok"))
            .andExpect(jsonPath("$.data.email").value("user@example.com"));
    }

    @Test
    void refresh_rotates_tokens() throws Exception {
        MvcResult login = mvc.perform(post("/api/auth/login")
                .contentType("application/json")
                .content("{\"email\":\"user@example.com\",\"password\":\"password1234\"}"))
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
    void logout_clears_cookie() throws Exception {
        MvcResult login = mvc.perform(post("/api/auth/login")
                .contentType("application/json")
                .content("{\"email\":\"user@example.com\",\"password\":\"password1234\"}"))
            .andExpect(status().isOk())
            .andReturn();

        Cookie refreshCookie = login.getResponse().getCookie("refresh_token");

        mvc.perform(post("/api/auth/logout").cookie(refreshCookie))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.SET_COOKIE,
                org.hamcrest.Matchers.containsString("Max-Age=0")));
    }
}

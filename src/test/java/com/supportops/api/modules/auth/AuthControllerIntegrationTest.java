package com.supportops.api.modules.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.supportops.api.common.util.AuthCookieUtil;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockCookie;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void registerShouldReturnAccessTokenAndSetRefreshCookie() throws Exception {
        String email = "user-" + UUID.randomUUID() + "@example.com";
        String payload = """
            {
              "email": "%s",
              "password": "Password@123",
              "firstName": "John",
              "lastName": "Doe",
              "organizationName": "Org One"
            }
            """.formatted(email);

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.data.expiresIn").value(900))
            .andExpect(jsonPath("$.data.user.email").value(email.toLowerCase()))
            .andExpect(cookie().exists(AuthCookieUtil.REFRESH_COOKIE_NAME));
    }

    @Test
    void refreshShouldRotateTokenAndLogoutShouldRevokeAndClearCookie() throws Exception {
        String email = "user-" + UUID.randomUUID() + "@example.com";
        String registerPayload = """
            {
              "email": "%s",
              "password": "Password@123",
              "firstName": "Jane",
              "lastName": "Doe",
              "organizationName": "Org Two"
            }
            """.formatted(email);

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerPayload))
            .andExpect(status().isOk());

        String loginPayload = """
            {
              "email": "%s",
              "password": "Password@123"
            }
            """.formatted(email);

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginPayload))
            .andExpect(status().isOk())
            .andExpect(cookie().exists(AuthCookieUtil.REFRESH_COOKIE_NAME))
            .andReturn();

        String loginSetCookie = loginResult.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        String loginRefreshToken = extractCookieValue(loginSetCookie, AuthCookieUtil.REFRESH_COOKIE_NAME);

        MvcResult refreshResult = mockMvc.perform(post("/api/v1/auth/refresh")
                .cookie(new MockCookie(AuthCookieUtil.REFRESH_COOKIE_NAME, loginRefreshToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
            .andExpect(cookie().exists(AuthCookieUtil.REFRESH_COOKIE_NAME))
            .andReturn();

        String refreshSetCookie = refreshResult.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        String rotatedRefreshToken = extractCookieValue(refreshSetCookie, AuthCookieUtil.REFRESH_COOKIE_NAME);

        Assertions.assertNotEquals(loginRefreshToken, rotatedRefreshToken);

        mockMvc.perform(post("/api/v1/auth/logout")
                .cookie(new MockCookie(AuthCookieUtil.REFRESH_COOKIE_NAME, rotatedRefreshToken)))
            .andExpect(status().isNoContent())
            .andExpect(cookie().maxAge(AuthCookieUtil.REFRESH_COOKIE_NAME, 0));

        mockMvc.perform(post("/api/v1/auth/refresh")
                .cookie(new MockCookie(AuthCookieUtil.REFRESH_COOKIE_NAME, rotatedRefreshToken)))
            .andExpect(status().isUnauthorized());
    }

    private String extractCookieValue(String setCookieHeader, String cookieName) {
        if (setCookieHeader == null || setCookieHeader.isBlank()) {
            throw new IllegalStateException("Missing Set-Cookie header");
        }

        String prefix = cookieName + "=";
        if (!setCookieHeader.startsWith(prefix)) {
            throw new IllegalStateException("Set-Cookie header does not contain expected cookie");
        }

        String valuePart = setCookieHeader.substring(prefix.length());
        int separatorIndex = valuePart.indexOf(';');
        return separatorIndex >= 0 ? valuePart.substring(0, separatorIndex) : valuePart;
    }
}

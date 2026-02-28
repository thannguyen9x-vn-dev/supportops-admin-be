package com.supportops.api.common.util;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

public final class AuthCookieUtil {

    public static final String REFRESH_COOKIE_NAME = "supportops_refresh_token";

    private AuthCookieUtil() {}

    public static void addRefreshCookie(
        HttpServletResponse response,
        String refreshToken,
        boolean secure,
        String sameSite,
        long maxAgeSeconds
    ) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE_NAME, refreshToken)
            .httpOnly(true)
            .secure(secure)
            .path("/")
            .sameSite(sameSite)
            .maxAge(maxAgeSeconds)
            .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public static void clearRefreshCookie(HttpServletResponse response, boolean secure, String sameSite) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE_NAME, "")
            .httpOnly(true)
            .secure(secure)
            .path("/")
            .sameSite(sameSite)
            .maxAge(0)
            .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}

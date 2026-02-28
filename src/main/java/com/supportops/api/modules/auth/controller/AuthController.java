package com.supportops.api.modules.auth.controller;

import com.supportops.api.common.dto.ApiResponse;
import com.supportops.api.common.exception.UnauthorizedException;
import com.supportops.api.common.util.AuthCookieUtil;
import com.supportops.api.config.AppAuthProperties;
import com.supportops.api.modules.auth.dto.LoginRequest;
import com.supportops.api.modules.auth.dto.LoginResponse;
import com.supportops.api.modules.auth.dto.RefreshTokenResponse;
import com.supportops.api.modules.auth.dto.RegisterRequest;
import com.supportops.api.modules.auth.dto.RegisterResponse;
import com.supportops.api.modules.auth.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.Arrays;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AppAuthProperties authProperties;

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        AuthService.LoginResult result = authService.login(request);
        AuthCookieUtil.addRefreshCookie(
            response,
            result.refreshToken(),
            authProperties.isCookieSecure(),
            authProperties.getCookieSameSite(),
            authProperties.getRefreshTokenTtlSeconds()
        );

        return ApiResponse.of(result.response());
    }

    @PostMapping("/register")
    public ApiResponse<RegisterResponse> register(@Valid @RequestBody RegisterRequest request, HttpServletResponse response) {
        AuthService.RegisterResult result = authService.register(request);
        AuthCookieUtil.addRefreshCookie(
            response,
            result.refreshToken(),
            authProperties.isCookieSecure(),
            authProperties.getCookieSameSite(),
            authProperties.getRefreshTokenTtlSeconds()
        );

        return ApiResponse.of(result.response());
    }

    @PostMapping("/refresh")
    public ApiResponse<RefreshTokenResponse> refresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = getRefreshTokenFromCookie(request)
            .orElseThrow(() -> new UnauthorizedException("Missing refresh token"));

        AuthService.RefreshResult result = authService.refresh(refreshToken);
        AuthCookieUtil.addRefreshCookie(
            response,
            result.refreshToken(),
            authProperties.isCookieSecure(),
            authProperties.getCookieSameSite(),
            authProperties.getRefreshTokenTtlSeconds()
        );

        return ApiResponse.of(result.response());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        getRefreshTokenFromCookie(request).ifPresent(authService::logout);

        AuthCookieUtil.clearRefreshCookie(
            response,
            authProperties.isCookieSecure(),
            authProperties.getCookieSameSite()
        );

        return ResponseEntity.noContent().build();
    }

    private Optional<String> getRefreshTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }

        return Arrays.stream(cookies)
            .filter(cookie -> AuthCookieUtil.REFRESH_COOKIE_NAME.equals(cookie.getName()))
            .map(Cookie::getValue)
            .findFirst();
    }
}

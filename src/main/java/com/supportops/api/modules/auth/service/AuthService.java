package com.supportops.api.modules.auth.service;

import com.supportops.api.common.exception.ConflictException;
import com.supportops.api.common.exception.UnauthorizedException;
import com.supportops.api.common.security.JwtUtil;
import com.supportops.api.common.util.HashUtil;
import com.supportops.api.config.AppAuthProperties;
import com.supportops.api.modules.auth.dto.LoginRequest;
import com.supportops.api.modules.auth.dto.LoginResponse;
import com.supportops.api.modules.auth.dto.RefreshTokenResponse;
import com.supportops.api.modules.auth.dto.RegisterRequest;
import com.supportops.api.modules.auth.dto.RegisterResponse;
import com.supportops.api.modules.auth.entity.RefreshToken;
import com.supportops.api.modules.auth.repository.RefreshTokenRepository;
import com.supportops.api.modules.user.dto.AuthUserResponse;
import com.supportops.api.modules.user.entity.User;
import com.supportops.api.modules.user.repository.UserRepository;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AppAuthProperties authProperties;

    @Transactional
    public LoginResult login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        if (!user.isActive() || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid credentials");
        }

        return issueTokensFor(user);
    }

    @Transactional
    public RegisterResult register(RegisterRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ConflictException("EMAIL_ALREADY_EXISTS", "Email already exists");
        }

        UUID tenantId = UUID.randomUUID();

        User user = new User();
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setRole("MEMBER");
        user.setTenantId(tenantId);
        user.setTenantName(request.organizationName());
        user.setActive(true);

        user = userRepository.save(user);
        TokenBundle bundle = issueTokenBundle(user);

        RegisterResponse response = new RegisterResponse(
                bundle.accessToken(),
                jwtUtil.getAccessTokenTtlSeconds(),
                toAuthUserResponse(user));

        return new RegisterResult(response, bundle.refreshToken());
    }

    @Transactional
    public RefreshResult refresh(String rawRefreshToken) {
        String hashed = HashUtil.sha256(rawRefreshToken);
        RefreshToken refreshToken = refreshTokenRepository
                .findByTokenHashAndRevokedAtIsNullAndExpiresAtAfter(hashed, Instant.now())
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        User user = refreshToken.getUser();
        if (!user.isActive()) {
            throw new UnauthorizedException("User is inactive");
        }

        refreshToken.setRevokedAt(Instant.now());
        refreshTokenRepository.save(refreshToken);

        TokenBundle bundle = issueTokenBundle(user);
        RefreshTokenResponse response = new RefreshTokenResponse(
                bundle.accessToken(),
                jwtUtil.getAccessTokenTtlSeconds());

        return new RefreshResult(response, bundle.refreshToken());
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        String hashed = HashUtil.sha256(rawRefreshToken);
        refreshTokenRepository.findByTokenHashAndRevokedAtIsNull(hashed)
                .ifPresent(token -> {
                    token.setRevokedAt(Instant.now());
                    refreshTokenRepository.save(token);
                });
    }

    private LoginResult issueTokensFor(User user) {
        TokenBundle bundle = issueTokenBundle(user);

        LoginResponse response = new LoginResponse(
                bundle.accessToken(),
                jwtUtil.getAccessTokenTtlSeconds(),
                toAuthUserResponse(user));

        return new LoginResult(response, bundle.refreshToken());
    }

    private TokenBundle issueTokenBundle(User user) {
        String accessToken = jwtUtil.generateAccessToken(user);
        String refreshTokenRaw = UUID.randomUUID() + "." + UUID.randomUUID();

        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setTokenHash(HashUtil.sha256(refreshTokenRaw));
        token.setExpiresAt(Instant.now().plusSeconds(authProperties.getRefreshTokenTtlSeconds()));
        refreshTokenRepository.save(token);

        return new TokenBundle(accessToken, refreshTokenRaw);
    }

    private AuthUserResponse toAuthUserResponse(User user) {
        return new AuthUserResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getAvatarUrl(),
                user.getRole(),
                user.getTenantId(),
                user.getTenantName());
    }

    public record LoginResult(LoginResponse response, String refreshToken) {
    }

    public record RegisterResult(RegisterResponse response, String refreshToken) {
    }

    public record RefreshResult(RefreshTokenResponse response, String refreshToken) {
    }

    private record TokenBundle(String accessToken, String refreshToken) {
    }
}

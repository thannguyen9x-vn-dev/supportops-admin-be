package com.supportops.api.modules.auth.dto;

public record RefreshTokenResponse(
    String accessToken,
    long expiresIn
) {}

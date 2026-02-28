package com.supportops.api.modules.auth.dto;

import com.supportops.api.modules.user.dto.AuthUserResponse;

public record RegisterResponse(
    String accessToken,
    long expiresIn,
    AuthUserResponse user
) {}

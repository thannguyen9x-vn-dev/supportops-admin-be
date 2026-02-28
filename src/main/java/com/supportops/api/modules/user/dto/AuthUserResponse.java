package com.supportops.api.modules.user.dto;

import java.util.UUID;

public record AuthUserResponse(
    UUID id,
    String email,
    String firstName,
    String lastName,
    String avatarUrl,
    String role,
    UUID tenantId,
    String tenantName
) {}

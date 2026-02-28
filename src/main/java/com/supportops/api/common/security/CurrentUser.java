package com.supportops.api.common.security;

import java.util.UUID;

public record CurrentUser(
    UUID userId,
    String email,
    String role,
    UUID tenantId,
    String tenantName
) {}

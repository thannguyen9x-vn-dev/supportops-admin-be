package com.supportops.api.modules.file.dto;

import java.time.Instant;

public record FileAccessUrlResponse(
    String url,
    Instant expiresAt
) {}

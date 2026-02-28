package com.supportops.api.modules.product.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductListResponse(
    UUID id,
    String name,
    String subtitle,
    String category,
    String brand,
    BigDecimal price,
    String thumbnailUrl,
    Instant createdAt,
    Instant updatedAt
) {}

package com.supportops.api.modules.product.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ProductResponse(
    UUID id,
    String name,
    String subtitle,
    String category,
    String brand,
    BigDecimal price,
    String details,
    List<ProductImageResponse> images,
    Instant createdAt,
    Instant updatedAt
) {}

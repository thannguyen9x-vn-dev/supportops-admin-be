package com.supportops.api.modules.product.dto;

import java.util.UUID;

public record ProductImageResponse(
    UUID id,
    String url,
    int sortOrder
) {}

package com.supportops.api.modules.product.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;

public record BulkDeleteProductsRequest(
    @NotEmpty(message = "Product ids are required")
    List<UUID> ids
) {}

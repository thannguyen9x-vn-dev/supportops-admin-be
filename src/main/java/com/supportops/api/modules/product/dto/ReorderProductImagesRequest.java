package com.supportops.api.modules.product.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;

public record ReorderProductImagesRequest(
    @NotEmpty(message = "imageIds is required")
    List<UUID> imageIds
) {}

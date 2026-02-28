package com.supportops.api.modules.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreateProductRequest(
    @NotBlank(message = "Product name is required")
    @Size(max = 255, message = "Product name must be <= 255 chars")
    String name,

    @Size(max = 255, message = "Subtitle must be <= 255 chars")
    String subtitle,

    @NotBlank(message = "Category is required")
    @Size(max = 100, message = "Category must be <= 100 chars")
    String category,

    @NotBlank(message = "Brand is required")
    @Size(max = 100, message = "Brand must be <= 100 chars")
    String brand,

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    BigDecimal price,

    @Size(max = 5000, message = "Details must be <= 5000 chars")
    String details
) {}

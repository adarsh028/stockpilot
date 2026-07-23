package com.stockpilot.product.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ProductRequest(
        @NotBlank @Size(max = 200) String name,
        String categoryId,
        @Size(max = 100) String brandName,
        String description,
        @Size(max = 500) String imageUrl,
        String status,
        @NotEmpty(message = "at least one SKU is required") @Valid List<SkuRequest> skus
) {
}

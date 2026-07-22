package com.stockpilot.product.dto;

import java.time.Instant;
import java.util.List;

public record ProductResponse(
        String id,
        String name,
        String category,
        String brandName,
        String description,
        String imageUrl,
        String status,
        List<SkuResponse> skus,
        Instant createdAt,
        Instant updatedAt
) {
}

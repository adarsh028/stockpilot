package com.stockpilot.product.dto;

public record SkuImageResponse(
        String id,
        String skuId,
        String url,
        boolean primary,
        int sortOrder
) {
}

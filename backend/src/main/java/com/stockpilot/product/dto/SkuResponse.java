package com.stockpilot.product.dto;

import java.math.BigDecimal;
import java.util.Map;

public record SkuResponse(
        String id,
        String productId,
        String sku,
        Map<String, String> attributes,
        BigDecimal costPrice,
        BigDecimal sellingPrice,
        Integer quantityOnHand,
        Integer reorderLevel
) {
}

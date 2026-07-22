package com.stockpilot.analytics.dto;

import java.math.BigDecimal;

public record TopProductResponse(
        String productId,
        String productName,
        String sku,
        long units,
        BigDecimal revenue
) {
}

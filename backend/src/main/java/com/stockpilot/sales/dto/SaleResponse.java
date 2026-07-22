package com.stockpilot.sales.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record SaleResponse(
        String id,
        String channelId,
        String channelName,
        String skuId,
        String sku,
        String productName,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal totalAmount,
        Instant saleDate,
        String marketplaceOrderId,
        String source,
        String status,
        List<String> warnings
) {
}

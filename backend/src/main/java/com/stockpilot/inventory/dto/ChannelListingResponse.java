package com.stockpilot.inventory.dto;

import java.math.BigDecimal;

public record ChannelListingResponse(
        String id,
        String channelId,
        String channelName,
        String skuId,
        String sku,
        String productName,
        String channelSku,
        int allocatedQuantity,
        BigDecimal channelPrice,
        String status
) {
}

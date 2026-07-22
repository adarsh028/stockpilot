package com.stockpilot.inventory.dto;

import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record ChannelListingUpdateRequest(
        String channelSku,
        @PositiveOrZero Integer allocatedQuantity,
        @PositiveOrZero BigDecimal channelPrice,
        String status
) {
}

package com.stockpilot.inventory.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.UUID;

public record ChannelListingRequest(
        @NotNull UUID skuId,
        String channelSku,
        @NotNull @PositiveOrZero Integer allocatedQuantity,
        @PositiveOrZero BigDecimal channelPrice,
        String status
) {
}

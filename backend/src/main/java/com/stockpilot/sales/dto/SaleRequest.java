package com.stockpilot.sales.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SaleRequest(
        @NotNull UUID channelId,
        @NotNull UUID skuId,
        @NotNull @Positive Integer quantity,
        @NotNull @PositiveOrZero BigDecimal unitPrice,
        Instant saleDate,
        String marketplaceOrderId
) {
}

package com.stockpilot.inventory.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Adjust stock for a SKU. Provide exactly one of delta (relative) or newQuantity (absolute).
 */
public record AdjustStockRequest(
        @NotNull UUID skuId,
        Integer delta,
        Integer newQuantity,
        String reason
) {
}

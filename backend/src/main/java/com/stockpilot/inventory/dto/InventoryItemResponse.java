package com.stockpilot.inventory.dto;

public record InventoryItemResponse(
        String id,
        String skuId,
        String sku,
        String productId,
        String productName,
        int quantityOnHand,
        int reorderLevel,
        int totalAllocated,
        boolean lowStock
) {
}

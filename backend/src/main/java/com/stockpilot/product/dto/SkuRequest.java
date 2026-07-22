package com.stockpilot.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Map;

public record SkuRequest(
        String id,
        @NotBlank @Size(max = 100) String sku,
        Map<String, String> attributes,
        @PositiveOrZero BigDecimal costPrice,
        @PositiveOrZero BigDecimal sellingPrice,
        @PositiveOrZero Integer quantityOnHand,
        @PositiveOrZero Integer reorderLevel
) {
}

package com.stockpilot.analytics.dto;

import java.math.BigDecimal;

public record SalesTrendPoint(
        String bucket,
        long units,
        BigDecimal revenue
) {
}

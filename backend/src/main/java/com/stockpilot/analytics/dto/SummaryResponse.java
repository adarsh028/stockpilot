package com.stockpilot.analytics.dto;

import java.math.BigDecimal;

public record SummaryResponse(
        long orderCount,
        long unitsSold,
        BigDecimal revenue,
        long lowStockCount,
        BigDecimal revenueChangePct,
        long orderCountPrevious,
        BigDecimal revenuePrevious,
        String from,
        String to
) {
}
